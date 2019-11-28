package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.callback.wrapper.ReadWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(ReadRequest.class)
public class ReadRequest<T extends BleDevice> implements ReadWrapperCallback {

    private BleReadCallback<T> bleReadCallback;
    private Ble<T> ble = Ble.getInstance();

    protected ReadRequest() {}

    public boolean read(T device, BleReadCallback<T> callback){
        this.bleReadCallback = callback;
        boolean result = false;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (Ble.getInstance() != null && bleRequest != null) {
            result = bleRequest.readCharacteristic(device.getBleAddress());
        }
        return result;
    }

    @Override
    public void onReadSuccess(BluetoothDevice device, BluetoothGattCharacteristic characteristic) {
        if(bleReadCallback != null){
            bleReadCallback.onReadSuccess(ble.getBleDevice(device), characteristic);
        }
    }

    @Override
    public void onReadFailed(BluetoothDevice device, String message) {
        if(bleReadCallback != null){
            bleReadCallback.onReadFailed(ble.getBleDevice(device), message);
        }
    }
}
