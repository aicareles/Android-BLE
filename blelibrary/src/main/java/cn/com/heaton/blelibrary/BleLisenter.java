package cn.com.heaton.blelibrary;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.BleVO.BleDevice;


/**This class is provide interface for BLE states
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
     * Scan to device
     * @param device ble device object
     * @param rssi rssi
     * @param scanRecord Bluetooth radio package
     */
    public abstract void onLeScan(BleDevice device, int rssi, byte[] scanRecord);

    /**
     *  When the write succeeds
     * @param gatt Bluetooth central device
     */
    public void onWrite(BluetoothGatt gatt){};

    /**
     *  Has been connected
     */
//    public void onConnected(BluetoothDevice device){};

//    public void onDisConnected(BluetoothDevice device){};

    /**
     *  When the MCU returns the data read
     * @param device ble device object
     */
    public void onRead(BluetoothDevice device){};

    /**
     *  MCU data sent to the app when the data callback call is setNotify
     * @param characteristic  characteristic
     */
    public void onChanged(BluetoothGattCharacteristic characteristic){};

    /**
     *  Set the notification here when the service finds a callback       setNotify
     * @param gatt gatt
     */
    public void onServicesDiscovered(BluetoothGatt gatt){};

    /**
     *  The callback is disconnected or connected when the connection is changed
     * @param device ble device object
     */
    public abstract void onConnectionChanged(BleDevice device);

    /**
     *  The notification describes when the write succeeded
     * @param gatt gatt
     */
    public void onDescriptorWriter(BluetoothGatt gatt){};

    /**
     *  Reads when the notification description is successful
     * @param gatt gatt
     */
    public void onDescriptorRead(BluetoothGatt gatt){};

    /**
     *  When the callback when the error, such as app can only connect four devices
     *  at the same time forcing the user to connect more than four devices will call back the method
     * @param errorCode errorCode
     */
    public void onError(int errorCode){};

    /**
     *  device connect timeout
     */
    public void onConnectTimeOut(){}

    /**
     *  Unable to initialize Bluetooth
     */
    public void onInitFailed(){}
}
