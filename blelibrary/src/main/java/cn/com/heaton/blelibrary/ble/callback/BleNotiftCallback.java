package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleNotiftCallback<T> {
    /**
     *  MCU data sent to the app when the data callback call is setNotify
     * @param device ble device object
     * @param characteristic  characteristic
     */
    public abstract void onChanged(T device, BluetoothGattCharacteristic characteristic);

    /**
     *  Set the notification feature to be successful and can send data
     * @param device ble device object
     */
    public void onReady(T device){};

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param gatt
     */
    public void onServicesDiscovered(BluetoothGatt gatt){};

    public void onNotifySuccess(BluetoothGatt gatt){};
}
