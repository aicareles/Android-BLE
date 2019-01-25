package cn.com.heaton.blelibrary.ble.callback;

import android.bluetooth.BluetoothGattCharacteristic;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleWriteEntityCallback<T> {
    public abstract void onWriteSuccess();
    public abstract void onWriteFailed();
    public void onWriteProgress(double progress){};
    public void onWriteCancel(){};
}
