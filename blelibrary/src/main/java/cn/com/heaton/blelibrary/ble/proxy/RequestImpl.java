package cn.com.heaton.blelibrary.ble.proxy;

import android.os.Build;
import android.support.annotation.RequiresApi;

import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.request.AdvertiserRequest;
import cn.com.heaton.blelibrary.ble.request.*;

/**
 *
 * Created by LiuLei on 2017/10/30.
 */

public class RequestImpl<T extends BleDevice> implements RequestLisenter<T>{

    public static RequestImpl newRequestImpl(){
        return new RequestImpl();
    }

    @Override
    public void startScan(BleScanCallback<T> callback) {
        ScanRequest<T> request = Rproxy.getRequest(ScanRequest.class);
        request.startScan(callback, Ble.options().scanPeriod);
    }

    @Override
    public void stopScan() {
        ScanRequest request = Rproxy.getRequest(ScanRequest.class);
        request.stopScan();
    }

    @Override
    public boolean connect(T device, BleConnectCallback<T> callback) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        return request.connect(device, callback);
    }

    @Override
    public boolean connect(String address, BleConnectCallback<T> callback) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        return request.connect(address, callback);
    }

    @Override
    public void notify(T device, BleNotiftCallback<T> callback) {
        NotifyRequest<T> request = Rproxy.getRequest(NotifyRequest.class);
        request.notify(device, callback);
    }

    @Override
    public void cancelNotify(T device, BleNotiftCallback<T> callback) {
        NotifyRequest<T> request = Rproxy.getRequest(NotifyRequest.class);
        request.cancelNotify(device, callback);
    }

    @Override
    public void disconnect(T device) {
        ConnectRequest request = Rproxy.getRequest(ConnectRequest.class);
        request.disconnect(device);
    }

    @Override
    public void disconnect(T device, BleConnectCallback<T> callback) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        request.disconnect(device, callback);
    }

    @Override
    public boolean read(T device, BleReadCallback<T> callback) {
        ReadRequest<T> request = Rproxy.getRequest(ReadRequest.class);
        return request.read(device, callback);
    }

    @Override
    public boolean readRssi(T device, BleReadRssiCallback<T> callback) {
        ReadRssiRequest<T> request = Rproxy.getRequest(ReadRssiRequest.class);
        return request.readRssi(device, callback);
    }

    @Override
    public boolean write(T device, byte[] data, BleWriteCallback<T> callback) {
        WriteRequest<T> request = Rproxy.getRequest(WriteRequest.class);
        return request.write(device, data, callback);
    }

    @Override
    public void writeEntity(T device, byte[] data, int packLength, int delay, BleWriteEntityCallback<T> callback) {
        WriteRequest<T> request = Rproxy.getRequest(WriteRequest.class);
        request.writeEntity(device, data, packLength, delay, callback);
    }

    @Override
    public void writeEntity(EntityData entityData, BleWriteEntityCallback<T> callback) {
        WriteRequest<T> request = Rproxy.getRequest(WriteRequest.class);
        request.writeEntity(entityData, callback);
    }

    @Override
    public void cancelWriteEntity() {
        WriteRequest<T> request = Rproxy.getRequest(WriteRequest.class);
        request.cancelWriteEntity();
    }

    @Override
    public boolean setMtu(String address, int mtu, BleMtuCallback<T> callback) {
        MtuRequest<T> request = Rproxy.getRequest(MtuRequest.class);
        return request.setMtu(address, mtu, callback);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void startAdvertising(byte[] payload) {
        AdvertiserRequest<T> request = Rproxy.getRequest(AdvertiserRequest.class);
        request.startAdvertising(payload);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void stopAdvertising() {
        AdvertiserRequest<T> request = Rproxy.getRequest(AdvertiserRequest.class);
        request.stopAdvertising();
    }
}
