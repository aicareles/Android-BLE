package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGatt;

import java.util.List;

/**
 * Created by jerry on 2019/1/29.
 */

public interface ConnectWrapperCallback<T> {
    void onConnectionChanged(T device);

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     *  @param device ble device object
     */
    void onConnectException(T device, int errorCode);

    void onConnectTimeOut(T device);

    /**
     *  Set the notification feature to be successful and can send data
     * @param device ble device object
     */
    void onReady(T device);

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param device
     */
    void onServicesDiscovered(T device, BluetoothGatt gatt);
}
