package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public interface WriteWrapperCallback {
    void onWriteSuccess(BluetoothDevice device, BluetoothGattCharacteristic characteristic);
    void onWiteFailed(BluetoothDevice device, String message);
}
