package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothDevice;

/**
 * Created by LiuLei on 2017/10/23.
 */

public interface ReadRssiWrapperCallback {

    void onReadRssiSuccess(BluetoothDevice device, int rssi);
}
