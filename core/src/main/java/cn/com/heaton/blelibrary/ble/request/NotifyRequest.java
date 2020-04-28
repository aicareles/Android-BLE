package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGattCharacteristic;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleNotiyCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.NotifyWrapperCallback;

/**
 * Created by LiuLei on 2017/10/23.
 */
@Implement(NotifyRequest.class)
public class NotifyRequest<T extends BleDevice> implements NotifyWrapperCallback<T> {

    private static final String TAG = "NotifyRequest";
    private BleNotiyCallback<T> notiftCallback;
    private BleWrapperCallback<T> bleWrapperCallback;

    protected NotifyRequest() {
        bleWrapperCallback = Ble.options().bleWrapperCallback;
    }

    public void notify(T device, boolean enable, BleNotiyCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), enable);
    }

    public void notifyByUuid(T device, boolean enable, UUID serviceUUID, UUID characteristicUUID, BleNotiyCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotificationByUuid(device.getBleAddress(),enable, serviceUUID, characteristicUUID);
    }

    @Deprecated
    public void cancelNotify(T device, BleNotiyCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), false);
    }

    @Override
    public void onChanged(final T device, final BluetoothGattCharacteristic characteristic) {
        if (null != notiftCallback){
            notiftCallback.onChanged(device, characteristic);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onChanged(device, characteristic);
        }
    }

    @Override
    public void onNotifySuccess(final T device) {
        if (null != notiftCallback){
            notiftCallback.onNotifySuccess(device);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onNotifySuccess(device);
        }
    }

    @Override
    public void onNotifyCanceled(T device) {
        if (null != notiftCallback){
            notiftCallback.onNotifyCanceled(device);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onNotifyCanceled(device);
        }
    }
}
