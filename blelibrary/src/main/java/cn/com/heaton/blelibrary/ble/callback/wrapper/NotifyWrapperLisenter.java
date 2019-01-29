package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by jerry on 2019/1/29.
 */

public interface NotifyWrapperLisenter<T> {

    void onChanged(T device, BluetoothGattCharacteristic characteristic);

    /**
     *  Set the notification feature to be successful and can send data
     * @param device ble device object
     */
    void onReady(T device);

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param gatt
     */
    void onServicesDiscovered(BluetoothGatt gatt);

    void onNotifySuccess(BluetoothGatt gatt);
}
