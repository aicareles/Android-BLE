package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;

/**
 * Created by LiuLei on 2017/10/23.
 */

public interface DescWrapperCallback<T> {

    void onDescReadSuccess(T device, BluetoothGattDescriptor descriptor);
    void onDescReadFailed(T device, int failedCode);

    void onDescWriteSuccess(T device, BluetoothGattDescriptor descriptor);
    void onDescWriteFailed(T device, int failedCode);
}
