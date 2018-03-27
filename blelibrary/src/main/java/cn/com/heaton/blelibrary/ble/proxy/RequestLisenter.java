package cn.com.heaton.blelibrary.ble.proxy;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;

/**
 *
 * Created by LiuLei on 2017/10/30.
 */

public interface RequestLisenter<T> {

    void startScan(BleScanCallback<T> callback);

    void stopScan();

    boolean connect(T device, BleConnCallback<T> callback);

    void notify(T device, BleNotiftCallback<T> callback);

    void disconnect(T device);

    void disconnect(T device, BleConnCallback<T> callback);

    boolean read(T device, BleReadCallback<T> callback);

    boolean readRssi(T device, BleReadRssiCallback<T> callback);

    boolean write(T device, byte[]data, BleWriteCallback<T> callback);

}
