package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;

/**
 * Created by LiuLei on 2017/5/3.
 * Bluetooth factory
 */

public class BleFactory{

    public BleFactory(){
    }

    public BleDevice create(Ble bleManager, BluetoothDevice device){
        return bleManager.getBleDevice(device);
    }

}
