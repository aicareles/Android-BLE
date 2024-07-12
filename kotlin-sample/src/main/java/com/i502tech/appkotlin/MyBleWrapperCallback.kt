package com.i502tech.appkotlin

import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.util.Log
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import cn.com.heaton.blelibrary.ble.utils.ByteUtils

/**
 * author: jerry
 * date: 20-4-13
 * email: superliu0911@gmail.com
 * des: 例： OTA升级可以再这里实现,与项目其他功能逻辑完全解耦
 */
class MyBleWrapperCallback : BleWrapperCallback<BleDevice>() {
    override fun onChanged(device: BleDevice, characteristic: BluetoothGattCharacteristic) {
        super.onChanged(device, characteristic)
        Log.d(TAG, "onChanged: " + ByteUtils.toHexString(characteristic.value))
    }

    override fun onServicesDiscovered(device: BleDevice, gatt: BluetoothGatt) {
        super.onServicesDiscovered(device, gatt)
        Log.d(TAG, "onServicesDiscovered: ")
    }

    override fun onWriteSuccess(device: BleDevice, characteristic: BluetoothGattCharacteristic) {
        super.onWriteSuccess(device, characteristic)
        Log.d(TAG, "onWriteSuccess: ")
    }

    override fun onConnectionChanged(device: BleDevice) {
        super.onConnectionChanged(device)
        Log.d(TAG, "onConnectionChanged: $device")
    }

    override fun onStop() {
        super.onStop()
        Log.d(TAG, "onStop: ")
    }

    override fun onNotifyFailed(device: BleDevice?, failedCode: Int) {

    }

    override fun onLeScan(device: BleDevice, rssi: Int, scanRecord: ByteArray) {
        super.onLeScan(device, rssi, scanRecord)
        Log.d(TAG, "onLeScan: $device")
    }

    override fun onNotifySuccess(device: BleDevice) {
        super.onNotifySuccess(device)
        Log.d(TAG, "onNotifySuccess: ")
    }

    override fun onNotifyCanceled(device: BleDevice) {
        super.onNotifyCanceled(device)
        Log.d(TAG, "onNotifyCanceled: ")
    }

    override fun onReady(device: BleDevice) {
        super.onReady(device)
        Log.d(TAG, "onReady: ")
    }

    override fun onDescWriteSuccess(device: BleDevice, descriptor: BluetoothGattDescriptor) {
        super.onDescWriteSuccess(device, descriptor)
    }

    override fun onDescWriteFailed(device: BleDevice, failedCode: Int) {
        super.onDescWriteFailed(device, failedCode)
    }

    override fun onDescReadFailed(device: BleDevice, failedCode: Int) {
        super.onDescReadFailed(device, failedCode)
    }

    override fun onDescReadSuccess(device: BleDevice, descriptor: BluetoothGattDescriptor) {
        super.onDescReadSuccess(device, descriptor)
    }

    override fun onMtuChanged(device: BleDevice, mtu: Int, status: Int) {
        super.onMtuChanged(device, mtu, status)
    }

    override fun onReadSuccess(device: BleDevice, characteristic: BluetoothGattCharacteristic) {
        super.onReadSuccess(device, characteristic)
    }

    companion object {
        private const val TAG = "MyBleWrapperCallback"
    }
}