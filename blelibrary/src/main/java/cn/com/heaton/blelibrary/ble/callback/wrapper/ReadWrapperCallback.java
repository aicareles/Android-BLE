package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by LiuLei on 2017/10/23.
 */

public interface ReadWrapperCallback<T> {

    void onReadSuccess(T device, BluetoothGattCharacteristic characteristic);
    void onReadFailed(T device, String message);

}
