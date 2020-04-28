package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGattCharacteristic;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.Callable;

import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.WriteWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.exception.BleWriteException;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(WriteRequest.class)
public class WriteRequest<T extends BleDevice> implements WriteWrapperCallback<T> {

    private BleWriteCallback<T> bleWriteCallback;
    private BleWriteEntityCallback<T> bleWriteEntityCallback;
    private boolean isWritingEntity;
    private boolean isAutoWriteMode = false;//当前是否为自动写入模式
    private final Object lock = new Object();
    private BleWrapperCallback<T> bleWrapperCallback;

    protected WriteRequest() {
        bleWrapperCallback = Ble.options().bleWrapperCallback;
    }

    public boolean write(T device, byte[]data, BleWriteCallback<T> callback){
        this.bleWriteCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        return bleRequest.wirteCharacteristic(device.getBleAddress(),data);
    }

    public boolean writeByUuid(T device, byte[]data, UUID serviceUUID, UUID characteristicUUID, BleWriteCallback<T> callback){
        this.bleWriteCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        return bleRequest.wirteCharacteristicByUuid(device.getBleAddress(), data, serviceUUID, characteristicUUID);
    }

    /*public void writeAsyn(final T device, final byte[]data, BleWriteCallback<T> lisenter){
        this.bleWriteCallback = lisenter;
        ThreadUtils.executeTask(new Runnable() {
            @Override
            public void run() {
                BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
                if (bleRequest != null) {
                    bleRequest.wirteCharacteristic(device.getBleAddress(),data);
                }
            }
        });
    }*/

    public void cancelWriteEntity(){
        if (isWritingEntity){
            isWritingEntity = false;
            isAutoWriteMode = false;
        }
    }

    public void writeEntity(EntityData entityData, BleWriteEntityCallback<T> callback) {
        EntityData.validParms(entityData);
        this.bleWriteEntityCallback = callback;
        executeEntity(entityData);
    }

    public void writeEntity(final T device, final byte[]data, final int packLength, final int delay, BleWriteEntityCallback<T> callback){
        this.bleWriteEntityCallback = callback;
        if(data == null || data.length == 0) {
            throw new BleWriteException("Send Entity cannot be empty");
        }
        if (packLength <= 0) {
            throw new BleWriteException("The data length per packet cannot be less than 0");
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
        final BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
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
                        if (bleWriteEntityCallback != null){
                            bleWriteEntityCallback.onWriteCancel();
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
                    boolean result = bleRequest.wirteCharacteristic(address, txBuffer);
                    if(!result){
                        if(bleWriteEntityCallback != null){
                            bleWriteEntityCallback.onWriteFailed();
                            isWritingEntity = false;
                            isAutoWriteMode = false;
                            return false;
                        }
                    }else {
                        if (bleWriteEntityCallback != null){
                            double progress = new BigDecimal((float)index / length).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                            bleWriteEntityCallback.onWriteProgress(progress);
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
                if(bleWriteEntityCallback != null){
                    bleWriteEntityCallback.onWriteSuccess();
                    isWritingEntity = false;
                    isAutoWriteMode = false;
                }
                return true;
            }
        };
        ThreadUtils.submit(callable);
    }

    @Override
    public void onWriteSuccess(T device, BluetoothGattCharacteristic characteristic) {
        if(bleWriteCallback != null){
            bleWriteCallback.onWriteSuccess(device, characteristic);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onWriteSuccess(device, characteristic);
        }
        if (isAutoWriteMode){
            synchronized (lock){
                lock.notify();
            }
        }
    }

    @Override
    public void onWriteFailed(T device, int failedCode) {
        if(bleWriteCallback != null){
            bleWriteCallback.onWriteFailed(device, failedCode);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onWriteFailed(device, failedCode);
        }
    }

}
