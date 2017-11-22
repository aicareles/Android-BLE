package cn.com.heaton.blelibrary.ble.callback;

import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleConnCallback<T> {
    /**
     *  The callback is disconnected or connected when the connection is changed
     * @param device ble device object
     */
    public abstract void onConnectionChanged(BleDevice device);

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     *  @param device ble device object
     * @param errorCode errorCode
     */
    public void onConnectException(BleDevice device,int errorCode){};
}
