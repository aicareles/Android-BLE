package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by jerry on 2019/1/29.
 */

public interface NotifyWrapperCallback {

    void onChanged(BluetoothDevice device, BluetoothGattCharacteristic characteristic);

    void onNotifySuccess(BluetoothDevice device);

    void onNotifyCanceled(BluetoothDevice device);
}
