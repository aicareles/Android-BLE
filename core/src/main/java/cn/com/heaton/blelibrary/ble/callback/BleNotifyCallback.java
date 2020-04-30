package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleNotifyCallback<T> {
    /**
     *  MCU data sent to the app when the data callback call is setNotify
     * @param device ble device object
     * @param characteristic  characteristic
     */
    public abstract void onChanged(T device, BluetoothGattCharacteristic characteristic);

    public void onNotifySuccess(T device){}

    public void onNotifyCanceled(T device){}

}
