package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.NotifyWrapperCallback;

/**
 * Created by LiuLei on 2017/10/23.
 */
@Implement(NotifyRequest.class)
public class NotifyRequest<T extends BleDevice> implements NotifyWrapperCallback<T> {

    private static final String TAG = "NotifyRequest";
    private BleNotiftCallback<T> notiftCallback;
    private BleWrapperCallback<T> bleWrapperCallback;

    protected NotifyRequest() {
        bleWrapperCallback = Ble.options().bleWrapperCallback;
    }

    public void notify(T device, BleNotiftCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), true);
    }

    public void cancelNotify(T device, BleNotiftCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), false);
    }

    @Override
    public void onChanged(final T device, final BluetoothGattCharacteristic characteristic) {
        if (null != notiftCallback){
//            T bleDevice = ble.getBleDevice(device);
            notiftCallback.onChanged(device, characteristic);
        }
        bleWrapperCallback.onChanged(device, characteristic);
    }

    @Override
    public void onNotifySuccess(final T device) {
        if (null != notiftCallback){
//            T bleDevice = ble.getBleDevice(device);
            notiftCallback.onNotifySuccess(device);
        }
        bleWrapperCallback.onNotifySuccess(device);
    }

    @Override
    public void onNotifyCanceled(T device) {
        if (null != notiftCallback){
//            T bleDevice = ble.getBleDevice(device);
            notiftCallback.onNotifyCanceled(device);
        }
        bleWrapperCallback.onNotifyCanceled(device);
    }
}
