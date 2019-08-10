package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Message;

import java.math.BigDecimal;
import java.util.concurrent.Callable;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.utils.TaskExecutor;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.exception.BleException;
import cn.com.heaton.blelibrary.ble.exception.BleWriteException;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(WriteRequest.class)
public class WriteRequest<T extends BleDevice> implements IMessage {

    private BleWriteCallback<T> mBleLisenter;
    private BleWriteEntityCallback<T> mBleEntityLisenter;
    private boolean isWritingEntity;
    private boolean isAutoWriteMode = false;//当前是否为自动写入模式
    private final Object lock = new Object();

    protected WriteRequest() {
        BleHandler.of().setHandlerCallback(this);
    }

    public boolean write(T device,byte[]data, BleWriteCallback<T> lisenter){
        this.mBleLisenter = lisenter;
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            result = service.wirteCharacteristic(device.getBleAddress(),data);
        }
        return result;
    }

    public void cancelWriteEntity(){
        if (isWritingEntity){
            isWritingEntity = false;
            isAutoWriteMode = false;
        }
    }

    public void writeEntity(EntityData entityData, BleWriteEntityCallback<T> lisenter) {
        try {
            EntityData.validParms(entityData);
        } catch (BleWriteException e) {
            e.printStackTrace();
        }
        this.mBleEntityLisenter = lisenter;
        executeEntity(entityData);
    }

    public void writeEntity(final T device, final byte[]data, final int packLength, final int delay, BleWriteEntityCallback<T> lisenter){
        this.mBleEntityLisenter = lisenter;
        if(data == null || data.length == 0) try {
            throw new BleWriteException("Send Entity cannot be empty");
        } catch (BleException e) {
            e.printStackTrace();
        }
        if (packLength <= 0) try {
            throw new BleWriteException("The data length per packet cannot be less than 0");
        } catch (BleWriteException e) {
            e.printStackTrace();
        }
        EntityData entityData = new EntityData(device.getBleAddress(), data, packLength, delay);
        executeEntity(entityData);
    }

    private void executeEntity(EntityData entityData) {
        final boolean autoWriteMode = entityData.isAutoWriteMode();
        final byte[] data = entityData.getData();
        final int packLength = entityData.getPackLength();
        final String address = entityData.getAddress();
        final long delay = entityData.getDelay();
        final boolean lastPackComplete = entityData.isLastPackComplete();
        final BluetoothLeService service = Ble.getInstance().getBleService();
        Callable<Boolean> callable = new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                isWritingEntity = true;
                isAutoWriteMode = autoWriteMode;
                int index = 0;
                int length = data.length;
                int availableLength = length;
                while (index < length){
                    if (!isWritingEntity){
                        if (mBleEntityLisenter != null){
                            mBleEntityLisenter.onWriteCancel();
                            isAutoWriteMode = false;
                        }
                        return false;
                    }
                    int onePackLength = packLength;
                    if (!lastPackComplete){//最后一包不足数据字节不会自动补零
                        onePackLength = (availableLength >= packLength ? packLength : availableLength);
                    }
                    byte[] txBuffer = new byte[onePackLength];
                    for (int i=0; i<onePackLength; i++){
                        if(index < length){
                            txBuffer[i] = data[index++];
                        }
                    }
                    availableLength-=onePackLength;
                    boolean result = service.wirteCharacteristic(address, txBuffer);
                    if(!result){
                        if(mBleEntityLisenter != null){
                            mBleEntityLisenter.onWriteFailed();
                            isWritingEntity = false;
                            isAutoWriteMode = false;
                            return false;
                        }
                    }else {
                        if (mBleEntityLisenter != null){
                            double progress = new BigDecimal((float)index / length).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            mBleEntityLisenter.onWriteProgress(progress);
                        }
                    }
                    if (autoWriteMode){
                        synchronized (lock){
                            lock.wait(500);
                        }
                    }else {
                        try {
                            Thread.sleep(delay);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if(mBleEntityLisenter != null){
                    mBleEntityLisenter.onWriteSuccess();
                    isWritingEntity = false;
                    isAutoWriteMode = false;
                }
                return true;
            }
        };
        TaskExecutor.submit(callable);
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case BleStates.BleStatus.Write:
                if(msg.obj instanceof BluetoothGattCharacteristic){
                    BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;
                    if(mBleLisenter != null){
                        mBleLisenter.onWriteSuccess(characteristic);
                    }
                    if (isAutoWriteMode){
                        synchronized (lock){
                            lock.notify();
                        }
                    }
                }
                break;
            default:
                break;
        }
    }
}
