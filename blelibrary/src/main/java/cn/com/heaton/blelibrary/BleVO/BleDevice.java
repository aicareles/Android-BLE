package cn.com.heaton.blelibrary.BleVO;

import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.BleConfig;

/**
 * Created by admin on 2016/11/26.
 * 如果需要设置蓝牙对象的更多属性与行为   则可继承自该类
 */

public class BleDevice {

    public final static String          TAG                      = BleDevice.class.getSimpleName();

    //是否被连接
    private boolean isConnected = false;
    //连接状态  2503未连接     2504正在连接中   2505已连接  2506已断开
    private int mConnectionState = BleConfig.DISCONNECT;

    //蓝牙地址
    private String mBleAddress;

    //蓝牙名称
    private String mBleName;
    //蓝牙修改后的名称
    private String mBleAlias;
    private List<BleDevice>mConnectedDevices = new ArrayList<>();


    public BleDevice(BluetoothDevice device) {
        this.mBleAddress = device.getAddress();
        this.mBleName = device.getName();
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(boolean connected) {
        isConnected = connected;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(int mConnectionState) {
        this.mConnectionState = mConnectionState;
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
