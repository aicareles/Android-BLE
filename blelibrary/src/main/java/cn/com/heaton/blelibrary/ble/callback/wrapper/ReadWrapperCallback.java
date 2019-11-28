package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by LiuLei on 2017/10/23.
 */

public interface ReadWrapperCallback {

    void onReadSuccess(BluetoothDevice device, BluetoothGattCharacteristic characteristic);
    void onReadFailed(BluetoothDevice device, String message);

}
