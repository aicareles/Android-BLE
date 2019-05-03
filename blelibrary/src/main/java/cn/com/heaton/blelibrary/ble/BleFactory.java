package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.BleDevice;

/**
 * 蓝牙工厂类
 * Created by LiuLei on 2017/5/3.
 */

public class BleFactory<T extends BleDevice> {

    protected BleFactory() {
    }

    public static <T extends BleDevice> T create(Class<T> cls, BluetoothDevice device) {
//        if(cls == ble.getClassType()){
        try {
            Constructor constructor = cls.getDeclaredConstructor(BluetoothDevice.class);
            constructor.setAccessible(true);
            try {
                T newDevice = (T) constructor.newInstance(device);
                newDevice.setAutoConnect(Ble.options().autoConnect);
                return newDevice;
            } catch (InstantiationException e) {
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
//        }
        throw new ClassCastException("Class must implements BleDevice");
    }

}
