package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
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
public class ReadRequest<T extends BleDevice> implements ReadWrapperCallback<T> {

    private BleReadCallback<T> bleReadCallback;
    private final BleWrapperCallback<T> bleWrapperCallback = Ble.options().getBleWrapperCallback();
    private final BleRequestImpl<T> bleRequest = BleRequestImpl.getBleRequest();

    public boolean read(T device, BleReadCallback<T> callback){
        this.bleReadCallback = callback;
        return bleRequest.readCharacteristic(device.getBleAddress());
    }

    public boolean readByUuid(T device, UUID serviceUUID, UUID characteristicUUID, BleReadCallback<T> callback){
        this.bleReadCallback = callback;
        return bleRequest.readCharacteristicByUuid(device.getBleAddress(), serviceUUID, characteristicUUID);
    }

    @Override
    public void onReadSuccess(T device, BluetoothGattCharacteristic characteristic) {
        if(bleReadCallback != null){
            bleReadCallback.onReadSuccess(device, characteristic);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onReadSuccess(device, characteristic);
        }
    }

    @Override
    public void onReadFailed(T device, int failedCode) {
        if(bleReadCallback != null){
            bleReadCallback.onReadFailed(device, failedCode);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onReadFailed(device, failedCode);
        }
    }
}
