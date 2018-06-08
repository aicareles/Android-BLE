package cn.com.heaton.blelibrary.ble.callback;

import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 * Created by LiuLei on 2018/6/2.
 */

public abstract class BleMtuCallback<T> {

    public void onMtuChanged(BleDevice device, int mtu, int status){}

}
