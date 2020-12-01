package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattService;

import java.util.List;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleConnectCallback<T> {
    /**
     *  The callback is disconnected or connected when the connection is changed
     * @param device ble device object
     */
    public abstract void onConnectionChanged(T device);

    public void onConnectCancel(T device){}

    /**
     *  Set the notification feature to be successful and can send data
     * @param device ble device object
     */
    public void onReady(T device){}

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param device
     */
    public void onServicesDiscovered(T device, BluetoothGatt gatt){}

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     *  @param device ble device object
     * @param errorCode errorCode
     */
    public void onConnectFailed(T device, int errorCode){}

}
