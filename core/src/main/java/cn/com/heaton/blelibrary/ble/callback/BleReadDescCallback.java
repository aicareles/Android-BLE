package cn.com.heaton.blelibrary.ble.callback;


import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleReadDescCallback<T> {

    public void onDescReadSuccess(T device, BluetoothGattDescriptor descriptor){}

    public void onDescReadFailed(T device, int failedCode){}

}
