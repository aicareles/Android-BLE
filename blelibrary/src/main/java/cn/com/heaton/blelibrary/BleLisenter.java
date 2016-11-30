package cn.com.heaton.blelibrary;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;

import cn.com.heaton.blelibrary.BleVO.BleDevice;


/**
 * Created by liulei on 2016/11/25.
 */

public abstract class BleLisenter {

    public void onStart(){};//开始扫描

    public void onStop(){};//停止扫描

    public abstract void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord);//扫描到设备

    public void onWrite(BluetoothGatt gatt,BluetoothGattCharacteristic characteristic, int status){};//当可写的时候

//    public void onConnected(BluetoothDevice device){};//已经被连接

//    public void onDisConnected(BluetoothDevice device){};//断开连接

    public void onRead(BluetoothDevice device){};//当读取到muc返回的数据

    public void onChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic){};//单片机数据改变时发送给app的数据回调  前提是setNotify

    public void onServicesDiscovered(BluetoothGatt gatt){};//当服务发现时的回调    //在此处设置通知  setNotify

    public abstract void onConnectionChanged(BluetoothGatt gatt,BleDevice device);//连接改变时的回调   断开或者连接

    public void onReady(BluetoothGatt gatt){};//

    public void onError(int errorCode){};//当错误时的回调   比如app只可同时连接4个设备时   用户强行连接4个以上的设备   就会回调该方法
}
