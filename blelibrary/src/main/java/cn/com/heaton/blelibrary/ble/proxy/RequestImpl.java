package cn.com.heaton.blelibrary.ble.proxy;

import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.request.NotifyRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRssiRequest;
import cn.com.heaton.blelibrary.ble.request.ScanRequest;
import cn.com.heaton.blelibrary.ble.request.WriteRequest;

/**
 *
 * Created by LiuLei on 2017/10/30.
 */

public class RequestImpl<T extends BleDevice> implements RequestLisenter<T>{

    private static RequestImpl instance = new RequestImpl();
    private static Ble.Options options;

    public static RequestImpl getInstance(Ble.Options opts){
        options = opts;
        return instance;
    }


    @Override
    public void startScan(BleScanCallback<T> callback) {
        ScanRequest<T> request = ScanRequest.getInstance();
        request.startScan(callback, options.scanPeriod);
    }

    @Override
    public void stopScan() {
        ScanRequest request = ScanRequest.getInstance();
        request.stopScan();
    }

    @Override
    public boolean connect(T device, BleConnCallback<T> callback) {
        ConnectRequest<T> request = ConnectRequest.getInstance();
        return request.connect(device, callback);
    }

    @Override
    public void notify(T device, BleNotiftCallback<T> callback) {
        NotifyRequest<T> request = NotifyRequest.getInstance();
        request.notify(device, callback);
    }


    @Override
    public void disconnect(T device) {
        ConnectRequest request = ConnectRequest.getInstance();
        request.disconnect(device);
    }

    @Override
    public void read(T device, BleReadCallback<T> callback) {
        ReadRequest<T> request = ReadRequest.getInstance();
        request.read(device, callback);
    }

    @Override
    public void readRssi(T device, BleReadRssiCallback<T> callback) {
        ReadRssiRequest<T> request = ReadRssiRequest.getInstance();
        request.readRssi(device, callback);
    }

    @Override
    public boolean write(T device, byte[] data, BleWriteCallback<T> callback) {
        WriteRequest<T> request = WriteRequest.getInstance();
        return request.write(device, data, callback);
    }
}
