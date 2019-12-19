package com.example.admin.mybledemo;

import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;

import cn.com.heaton.blelibrary.ble.callback.wrapper.DefaultBleWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class MyBleWrapperCallback extends DefaultBleWrapperCallback<BleDevice> {

    private static final String TAG = "MyBleWrapperCallback";

    @Override
    public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
        super.onChanged(device, characteristic);
        Log.d(TAG, "onChanged: "+ ByteUtils.toHexString(characteristic.getValue()));
    }

    @Override
    public void onConnectionChanged(BleDevice device) {
        super.onConnectionChanged(device);
        Log.d(TAG, "onConnectionChanged: "+device.toString());
    }

    @Override
    public void onStop() {
        super.onStop();
        Log.d(TAG, "onStop: ");
    }

    @Override
    public void onLeScan(BleDevice device, int rssi, byte[] scanRecord) {
        super.onLeScan(device, rssi, scanRecord);
        Log.d(TAG, "onLeScan: "+device.toString());
    }

    @Override
    public void onNotifySuccess(BleDevice device) {
        super.onNotifySuccess(device);
        Log.d(TAG, "onNotifySuccess: ");
    }

    @Override
    public void onReady(BleDevice device) {
        super.onReady(device);
//        Ble.getInstance().startNotify(device, null);
    }

    @Override
    public void onStart() {
        super.onStart();
        Log.d(TAG, "onStart: ");
    }
}
