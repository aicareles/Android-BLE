package cn.com.heaton.blelibrary;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.BleVO.BleDevice;


/**
 * Created by liulei on 2016/11/25.
 */

public abstract class BleLisenter {

    /**
     *  Start the scan
     */
    public void onStart(){};

    /**
     *  Stop scanning
     */
    public void onStop(){};

    /**
     *   Scan to device
     */
    public abstract void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);

    /**
     *  When the write succeeds
     */
    public void onWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status){};

    /**
     *  Has been connected
     */
//    public void onConnected(BluetoothDevice device){};

    /**
     *  Disconnect
     */
//    public void onDisConnected(BluetoothDevice device){};

    /**
     *  When the MCU returns the data read
     * @param device
     */
    public void onRead(BluetoothDevice device){};

    /**
     *  MCU data sent to the app when the data callback call is setNotify
     * @param gatt
     * @param characteristic
     */
    public void onChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){};

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param gatt
     */
    public void onServicesDiscovered(BluetoothGatt gatt){};

    /**
     *  The callback is disconnected or connected when the connection is changed
     * @param gatt
     * @param device
     */
    public abstract void onConnectionChanged(BluetoothGatt gatt,BleDevice device);

    /**
     *  The notification describes when the write succeeded
     * @param gatt
     */
    public void onDescriptorWriter(BluetoothGatt gatt){};

    /**
     *  Reads when the notification description is successful
     * @param gatt
     */
    public void onDescriptorRead(BluetoothGatt gatt){};

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     * @param errorCode
     */
    public void onError(int errorCode){};
}
