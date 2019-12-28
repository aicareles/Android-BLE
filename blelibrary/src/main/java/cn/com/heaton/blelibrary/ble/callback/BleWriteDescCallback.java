package cn.com.heaton.blelibrary.ble.callback;


import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleWriteDescCallback<T> {

    public void onWriteDescSuccess(T dedvice, BluetoothGattDescriptor descriptor){}

    public void onWriteDescFailed(T device, int failedCode){}

}
