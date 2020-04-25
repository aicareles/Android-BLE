package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleReadCallback<T> {

    public void onReadSuccess(T dedvice, BluetoothGattCharacteristic characteristic){}

    public void onReadFailed(T device, int failedCode){}

}
