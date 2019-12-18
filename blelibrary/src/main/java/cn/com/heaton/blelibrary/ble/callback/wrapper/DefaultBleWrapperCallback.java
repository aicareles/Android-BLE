package cn.com.heaton.blelibrary.ble.callback.wrapper;

import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.ble.model.BleDevice;

public class DefaultBleWrapperCallback<T extends BleDevice> extends BleWrapperCallback<T>{

    private final String TAG = getClass().getSimpleName();

    @Override
    public void onConnectionChanged(T device) {
    }

    @Override
    public void onConnectException(T device) {

    }

    @Override
    public void onConnectTimeOut(T device) {

    }

    @Override
    public void onReady(T device) {
    }

    @Override
    public void onServicesDiscovered(T device) {

    }

    @Override
    public void onChanged(T device, BluetoothGattCharacteristic characteristic) {
    }

    @Override
    public void onNotifySuccess(T device) {

    }

    @Override
    public void onNotifyCanceled(T device) {

    }

    @Override
    public void onWriteSuccess(T device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onWiteFailed(T device, String message) {

    }

    @Override
    public void onReadSuccess(T device, BluetoothGattCharacteristic characteristic) {

    }

    @Override
    public void onReadFailed(T device, String message) {

    }

    @Override
    public void onLeScan(T device, int rssi, byte[] scanRecord) {
    }
}
