package cn.com.heaton.blelibrary.ble.model;

public abstract class BleFactory<T extends BleDevice> {

    public T create(String address, String name) {
        return (T) new BleDevice(address, name);
    }

}
