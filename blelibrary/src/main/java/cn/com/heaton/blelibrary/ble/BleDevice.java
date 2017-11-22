package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;

import java.util.concurrent.atomic.AtomicBoolean;

import cn.com.heaton.blelibrary.ota.OtaListener;

/**
 * Created by LiuLei on 2016/11/26.
 * If you need to set the Bluetooth object more properties and behavior can be inherited from the class     such as extends BleDevice
 */

public class BleDevice {

    public final static String          TAG                      = BleDevice.class.getSimpleName();

    /**
     *  Connection Status:
     *  2503 Not Connected
     *  2504 Connected
     *  2505 Connected
     *  2506 Disconnected
     */
    private int mConnectionState = BleStates.BleStatus.DISCONNECT;

    /**
     *   Bluetooth address
     */
    private String mBleAddress;



    /**
     *  Bluetooth name
     */
    private String mBleName;
    /**
     *   Bluetooth modified name
     */
    private String mBleAlias;

    private boolean mAutoConnect = false;//默认自动连接

    /**
     * Use the address and name of the BluetoothDevice object
     * to construct the address and name of the {@code BleDevice} object
     *
     * @param device BleDevice
     */
    public BleDevice(BluetoothDevice device) {
        this.mBleAddress = device.getAddress();
        this.mBleName = device.getName();
    }

    public boolean isConnected() {
        return mConnectionState == BleStates.BleStatus.CONNECTED;
    }

    public boolean isConnectting() {
        return mConnectionState == BleStates.BleStatus.CONNECTING;
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public void setAutoConnect(boolean mAutoConnect) {
        this.mAutoConnect = mAutoConnect;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(@BleStates.BleStatus int state){
        mConnectionState = state;
    }


    public String getBleAddress() {
        return mBleAddress;
    }

    public void setBleAddress(String mBleAddress) {
        this.mBleAddress = mBleAddress;
    }

    public String getmBleName() {
        return mBleName;
    }

    public void setBleName(String mBleName) {
        this.mBleName = mBleName;
    }

    public String getBleAlias() {
        return mBleAlias;
    }

    public void setBleAlias(String mBleAlias) {
        this.mBleAlias = mBleAlias;
    }

}
