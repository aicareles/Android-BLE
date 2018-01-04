package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Message;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public class ReadRequest<T extends BleDevice> implements IRequest {

    private BleReadCallback<T> mBleLisenter;

    private static volatile ReadRequest instance;
    public static <T extends BleDevice> ReadRequest<T> getInstance(){
        if (instance == null) {
            synchronized (ReadRequest.class) {
                if (instance == null) {
                    instance = new ReadRequest();
                }
            }
        }
        return instance;
    }

    protected ReadRequest() {
        BleHandler handler = BleHandler.getHandler();
        handler.setHandlerCallback(this);
    }

    public boolean read(T device, BleReadCallback<T> lisenter){
        this.mBleLisenter = lisenter;
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (Ble.getInstance() != null && service != null) {
            result = service.readCharacteristic(device.getBleAddress());
        }
        return result;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case BleStates.BleStatus.Read:
                if(msg.obj instanceof BluetoothGattCharacteristic){
                    BluetoothGattCharacteristic characteristic = (BluetoothGattCharacteristic) msg.obj;
                    mBleLisenter.onReadSuccess(characteristic);
                }
                break;
            default:
                break;
        }
    }
}
