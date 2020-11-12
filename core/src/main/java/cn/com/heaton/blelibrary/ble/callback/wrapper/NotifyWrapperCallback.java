package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 * Created by jerry on 2019/1/29.
 */

public interface NotifyWrapperCallback<T> {

    void onChanged(T device, BluetoothGattCharacteristic characteristic);

    void onNotifySuccess(T device);

    void onNotifyFailed(T device, int failedCode);

    void onNotifyCanceled(T device);
}
