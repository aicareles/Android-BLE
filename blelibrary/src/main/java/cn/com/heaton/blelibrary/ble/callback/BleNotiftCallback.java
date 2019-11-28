package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.model.BleDevice;

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

    public void onNotifySuccess(T device){}

    public void onNotifyCanceled(T device){}

    public void onNotifySuccess(BleDevice device, BluetoothGatt gatt){}
}
