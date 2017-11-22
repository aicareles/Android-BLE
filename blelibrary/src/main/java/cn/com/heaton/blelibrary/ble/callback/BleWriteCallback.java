package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public interface BleWriteCallback<T> {
    void onWriteSuccess(BluetoothGattCharacteristic characteristic);
}
