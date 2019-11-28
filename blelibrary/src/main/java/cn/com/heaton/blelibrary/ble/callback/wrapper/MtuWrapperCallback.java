package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;

/**
 * Created by LiuLei on 2018/6/2.
 */

public interface MtuWrapperCallback {

    void onMtuChanged(BluetoothDevice device, int mtu, int status);

}
