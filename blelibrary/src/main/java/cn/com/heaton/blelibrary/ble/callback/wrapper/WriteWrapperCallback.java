package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public interface WriteWrapperCallback<T> {
    void onWriteSuccess(T device, BluetoothGattCharacteristic characteristic);
    void onWiteFailed(T device, String message);
}
