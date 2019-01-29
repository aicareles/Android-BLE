package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;

/**
 * Created by jerry on 2019/1/29.
 */

public interface ConnectWrapperLisenter {
    void onConnectionChanged(BluetoothDevice device, int status);

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     *  @param device ble device object
     */
    void onConnectException(BluetoothDevice device);

    void onConnectTimeOut(BluetoothDevice device);
}
