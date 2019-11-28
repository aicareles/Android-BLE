package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.TaskExecutor;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.NotifyWrapperCallback;

/**
 * Created by LiuLei on 2017/10/23.
 */
@Implement(NotifyRequest.class)
public class NotifyRequest<T extends BleDevice> implements NotifyWrapperCallback {

    private static final String TAG = "NotifyRequest";
    private BleNotiftCallback<T> notiftCallback;
    private Ble<T> ble = Ble.getInstance();

    protected NotifyRequest() {}

    public void notify(T device, BleNotiftCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), true);
    }

    public void cancelNotify(T device, BleNotiftCallback<T> callback) {
        notiftCallback = callback;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        bleRequest.setCharacteristicNotification(device.getBleAddress(), false);
    }

    @Override
    public void onChanged(final BluetoothDevice device, final BluetoothGattCharacteristic characteristic) {
        if (null != notiftCallback){
            T bleDevice = ble.getBleDevice(device);
            notiftCallback.onChanged(bleDevice, characteristic);
        }
    }

    @Override
    public void onNotifySuccess(final BluetoothDevice device) {
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != notiftCallback){
                    T bleDevice = ble.getBleDevice(device);
                    notiftCallback.onNotifySuccess(bleDevice);
                }
            }
        });
    }

    @Override
    public void onNotifyCanceled(final BluetoothDevice device) {
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != notiftCallback){
                    T bleDevice = ble.getBleDevice(device);
                    notiftCallback.onNotifyCanceled(bleDevice);
                }
            }
        });
    }
}
