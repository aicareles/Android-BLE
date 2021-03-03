/*
 * Copyright (C)  aicareles, Android-BLE Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package cn.com.heaton.blelibrary.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresApi;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotifyCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadDescCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteDescCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BluetoothChangedObserver;
import cn.com.heaton.blelibrary.ble.exception.BleException;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.BleFactory;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.queue.reconnect.DefaultReConnectHandler;
import cn.com.heaton.blelibrary.ble.queue.RequestTask;
import cn.com.heaton.blelibrary.ble.queue.WriteQueue;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.proxy.RequestImpl;
import cn.com.heaton.blelibrary.ble.proxy.RequestListener;
import cn.com.heaton.blelibrary.ble.proxy.RequestProxy;
import cn.com.heaton.blelibrary.ble.request.DescriptorRequest;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ble.request.ScanRequest;

/**
 * provides all bluetooth operation apis
 * @author aicareles
 * @since 2016/12/7
 */
public final class Ble<T extends BleDevice> {

    private final static String TAG = "Ble";
    private static volatile Ble sInstance;
    private static Options options;
    private Context context;
    private RequestListener<T> request;
    private final Object locker = new Object();
    private BleRequestImpl<T> bleRequestImpl;
    //打开蓝牙标志位
    public static final int REQUEST_ENABLE_BT = 1;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothChangedObserver bleObserver;

    public interface InitCallback {
        void success();
        void failed(int failedCode);
    }

    /**
     * Initializes a newly created {@code Ble} object so that it represents
     * a bluetooth management class .  Note that use of this constructor is
     * unnecessary since Can not be externally constructed.
     */
    private Ble(){}

    public static <T extends BleDevice> Ble<T> getInstance(){
        if (sInstance == null) {
            synchronized (Ble.class) {
                if (sInstance == null) {
                    sInstance = new Ble();
                }
            }
        }
        return sInstance;
    }

    /**
     *  bluetooth initialization
     * @param context context object
     */
    public void init(Context context, Options options, InitCallback callback) {
        if (context == null){
            throw new BleException("context is null");
        }
        if (this.context != null){
            BleLog.e(TAG, "Ble is Initialized!");
            if (callback != null){
                callback.failed(BleStates.InitAlready);
            }
            return;
        }
        this.context = context;
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (bluetoothAdapter == null){
            if (callback != null){
                BleLog.e(TAG, "bluetoothAdapter is not available!");
                callback.failed(BleStates.NotAvailable);
            }
            return;
        }
        if (!isSupportBle(context)){
            if (callback != null){
                BleLog.e(TAG, "not support ble!");
                callback.failed(BleStates.NotSupportBLE);
            }
            return;
        }
        Ble.options = (options == null ? options() : options);
        BleLog.init(Ble.options);
        request = (RequestListener<T>) RequestProxy.newProxy().bindProxy(context, RequestImpl.newRequestImpl());
        bleRequestImpl = BleRequestImpl.getBleRequest();
        bleRequestImpl.initialize(context);
        initBleObserver();
        BleLog.d(TAG, "Ble init success");
        if (callback != null){
            callback.success();
        }
    }

    public static <T extends BleDevice> Ble<T> create(Context context, InitCallback callback){
        return create(context, options(), callback);
    }

    public static <T extends BleDevice> Ble<T> create(Context context, Options options, InitCallback callback) {
        Ble<T> ble = getInstance();
        ble.init(context, options, callback);
        return ble;
    }

    /**
     * global bluetooth on off monitoring
     */
    public void setBleStatusCallback(BleStatusCallback callback){
        if (bleObserver != null) {
            bleObserver.setBleScanCallbackInner(callback);
        }
    }

    /**
     * start scanning
     */
    public void startScan(BleScanCallback<T> callback){
        request.startScan(callback, options().scanPeriod);
    }

    public void startScan(BleScanCallback<T> callback, long scanPeriod){
        request.startScan(callback, scanPeriod);
    }

    /**
     * stop scanning
     */
    public void stopScan(){
        request.stopScan();
    }

    /**
     * connect bluetooth
     */
    public void connect(T device, BleConnectCallback<T> callback) {
        synchronized (locker) {
            request.connect(device, callback);
        }
    }

    /**
     * connect to the device through the mac address
     */
    public void connect(String address,BleConnectCallback<T> callback){
        synchronized (locker) {
            request.connect(address, callback);
        }
    }

    public void connects(List<T> devices, BleConnectCallback<T> callback) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        request.connect(devices, callback);
    }

    public void cancelConnecting(T device){
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        request.cancelConnecting(device);
    }

    public void cancelConnectings(List<T> devices){
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        request.cancelConnectings(devices);
    }

    /**
     * set whether to automatically connect
     */
    public void autoConnect(T device, boolean autoConnect){
        DefaultReConnectHandler.provideReconnectHandler().resetAutoConnect(device, autoConnect);
    }

    public void cancelAutoConnects(){
        DefaultReConnectHandler.provideReconnectHandler().cancelAutoConnect();
    }

    /**
     * disconnect
     * @param device
     */
    public void disconnect(T device) {
        request.disconnect(device);
    }

    public void disconnect(T device, BleConnectCallback<T> callback) {
        request.disconnect(device, callback);
    }

    public void disconnectAll(){
        Collection<T> connectedDevices = getConnectedDevices();
        if (!connectedDevices.isEmpty()){
            for (T device: connectedDevices) {
                request.disconnect(device);
            }
        }
    }

    /**
     * 连接成功后，开始设置通知
     * @param device 蓝牙设备对象
     * @param callback 通知回调
     * @deprecated Use {@link Ble#enableNotify(T, boolean, BleNotifyCallback)} instead.
     */
    public void startNotify(T device, BleNotifyCallback<T> callback){
        request.notify(device, callback);
    }

    /**
     * 移除通知
     * @param  device 蓝牙设备对象
     * @deprecated Use {@link Ble#enableNotify(T, boolean, BleNotifyCallback)} instead.
     */
    public void cancelNotify(T device, BleNotifyCallback<T> callback){
        request.cancelNotify(device, callback);
    }

    /**
     * 设置通知
     * @param device 蓝牙设备对象
     * @param enable 打开/关闭
     * @param callback 通知回调
     */
    public void enableNotify(T device, boolean enable, BleNotifyCallback<T> callback){
        request.enableNotify(device, enable, callback);
    }

    /**
     * 通过uuid设置指定通知
     * @param device 蓝牙设备对象
     * @param enable 打开/关闭
     * @param serviceUUID 服务uuid
     * @param characteristicUUID 通知特征uuid
     * @param callback 通知回调
     */
    public void enableNotifyByUuid(T device, boolean enable, UUID serviceUUID, UUID characteristicUUID, BleNotifyCallback<T> callback){
        request.enableNotifyByUuid(device, enable, serviceUUID, characteristicUUID, callback);
    }

    /**
     * 读取数据
     * @param device 蓝牙设备对象
     * @param callback 读取结果回调
     */
    public boolean read(T device, BleReadCallback<T> callback){
        return request.read(device, callback);
    }

    /**
     * 写入到指定uuid数据
     * @param device 蓝牙设备对象
     * @param serviceUUID 服务uuid
     * @param characteristicUUID 写入特征uuid
     * @param callback 写入回调
     */
    public boolean readByUuid(T device, UUID serviceUUID, UUID characteristicUUID, BleReadCallback<T> callback){
        return request.readByUuid(device, serviceUUID, characteristicUUID, callback);
    }

    public boolean readDesByUuid(T device, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID, BleReadDescCallback<T> callback){
        DescriptorRequest<T> request = Rproxy.getRequest(DescriptorRequest.class);
        return request.readDes(device, serviceUUID, characteristicUUID, descriptorUUID, callback);
    }

    public boolean writeDesByUuid(T device, byte[] data, UUID serviceUUID, UUID characteristicUUID, UUID descriptorUUID, BleWriteDescCallback<T> callback){
        DescriptorRequest<T> request = Rproxy.getRequest(DescriptorRequest.class);
        return request.writeDes(device, data, serviceUUID, characteristicUUID, descriptorUUID, callback);
    }

    /**
     * 读取远程RSSI
     * @param device 蓝牙设备对象
     * @param callback 读取远程RSSI结果回调
     */
    public boolean readRssi(T device, BleReadRssiCallback<T> callback){
        return request.readRssi(device, callback);
    }

    /**
     * 设置MTU
     * @param address 蓝牙设备地址
     * @param mtu mtu大小
     * @return 是否设置成功
     */
    public boolean setMTU(String address, int mtu, BleMtuCallback<T> callback){
        return request.setMtu(address, mtu, callback);
    }

    /**
     * 写入数据
     * @param device 蓝牙设备对象
     * @param data 写入数据字节数组
     * @param callback 写入结果回调
     * @return 写入是否成功
     */
    public boolean write(T device, byte[]data, BleWriteCallback<T> callback){
        return request.write(device, data, callback);
    }

    /**
     * 写入到指定uuid数据
     * @param device 蓝牙设备对象
     * @param data 数据
     * @param serviceUUID 服务uuid
     * @param characteristicUUID 写入特征uuid
     * @param callback 写入回调
     */
    public boolean writeByUuid(T device, byte[]data, UUID serviceUUID, UUID characteristicUUID, BleWriteCallback<T> callback){
        return request.writeByUuid(device, data, serviceUUID, characteristicUUID, callback);
    }

    /**
     *
     * @param delay
     * @param task
     * @deprecated Use {@link Ble#writeQueue(RequestTask task)} instead.
     */
    public void writeQueueDelay(long delay, RequestTask task){
        writeQueue(task);
    }

    public void writeQueue(RequestTask task){
        WriteQueue.getInstance().put(task);
    }

    /**
     * 写入大数据量的数据（分包）
     * @param device 蓝牙设备对象
     * @param data 写入的总字节数组（如整个文件的字节数组）
     * @param packLength 每包需要发送的长度
     * @param delay 每包之间的时间间隔
     * @param callback 发送结果回调
     * @deprecated Use {@link Ble#writeEntity(EntityData, BleWriteEntityCallback)} instead.
     */
    public void writeEntity(T device, final byte[]data, @IntRange(from = 1,to = 512)int packLength, int delay, BleWriteEntityCallback<T> callback){
        request.writeEntity(device, data, packLength, delay, callback);
    }

    /**
     * 写入大数据量数据，需要延迟(分包)
     * 自动模式下写入大数据量数据，无需延迟，根据系统底层返回结果进行连续写入(分包)
     * @param entityData 数据实体
     * @param callback 写入回调
     */
    public void writeEntity(EntityData entityData, BleWriteEntityCallback<T> callback){
        request.writeEntity(entityData, callback);
    }

    public void cancelWriteEntity() {
        request.cancelWriteEntity();
    }

    /**
     * 获取自定义蓝牙服务对象
     * @return 自定义蓝牙服务对象
     */
    public BleRequestImpl getBleRequest() {
        return bleRequestImpl;
    }

    /**
     * 根据蓝牙地址获取蓝牙对象
     * @param address 蓝牙地址
     * @return 对应的蓝牙对象
     */
    public T getBleDevice(String address){
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        return request.getBleDevice(address);
    }

    /**
     * 获取对应蓝牙对象
     * @param device 原生蓝牙对象
     * @return 对应蓝牙对象
     */
    public T getBleDevice(BluetoothDevice device) {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        if(device != null){
            return request.getBleDevice(device.getAddress());
        }
        return null;
    }

    /**
     * 获取对应锁对象
     */
    public Object getLocker() {
        return locker;
    }

    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        ScanRequest request = Rproxy.getRequest(ScanRequest.class);
        return request.isScanning();
    }

    /**
     *
     * @return 已经连接的设备集合
     */

    public List<T> getConnectedDevices() {
        ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
        return request.getConnectedDevices();
    }

    /**
     * 释放所有资源
     */
    public void released(){
        releaseGatts();
        releaseBleObserver();
        if (isScanning())stopScan();
        bleRequestImpl.release();
        bleRequestImpl = null;
        Rproxy.release();
        context = null;
        BleLog.d(TAG, "AndroidBLE already released");
    }

    /**
     * Release Empty all resources
     */
    private void releaseGatts() {
        BleLog.d(TAG, "BluetoothGatts is released");
        synchronized (locker) {
            Collection<T> connectedDevices = getConnectedDevices();
            for (T bleDevice : connectedDevices) {
                disconnect(bleDevice);
            }
        }
    }

    private void initBleObserver(){
        if (bleObserver == null){
            bleObserver = new BluetoothChangedObserver(context);
            bleObserver.registerReceiver();
        }
    }

    private void releaseBleObserver() {
        BleLog.d(TAG, "BleObserver is released");
        if (bleObserver != null) {
            bleObserver.unregisterReceiver();
            bleObserver = null;
        }
    }

    /**
     * cancel Callback
     * @param callback (BleScanCallback、BleConnectCallback)
     */
    public void cancelCallback(Object callback){
        if (callback instanceof BleScanCallback){
            ScanRequest request = Rproxy.getRequest(ScanRequest.class);
            request.cancelScanCallback();
        }else if (callback instanceof BleConnectCallback){
            ConnectRequest<T> request = Rproxy.getRequest(ConnectRequest.class);
            request.cancelConnectCallback();
        }
    }

    private BluetoothAdapter getBluetoothAdapter(){
        if (bluetoothAdapter == null){
            bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        }
        return bluetoothAdapter;
    }

    /**
     *
     * @return 是否支持蓝牙
     */
    public boolean isSupportBle(Context context) {
        return (getBluetoothAdapter() != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    /**
     *
     * @return 蓝牙是否打开
     */
    public boolean isBleEnable() {
        BluetoothAdapter bluetoothAdapter = getBluetoothAdapter();
        return bluetoothAdapter != null && bluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙(默认模式--带系统弹出框)
     *
     * @param activity 上下文对象
     */
    public void turnOnBlueTooth(Activity activity) {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!isBleEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
    }

    /**
     * 强制打开蓝牙（不弹出系统弹出框）
     */
    public void turnOnBlueToothNo(){
        if(!isBleEnable()){
            if (bluetoothAdapter != null){
                bluetoothAdapter.enable();
            }
        }
    }

    /**
     * 关闭蓝牙
     */
    public boolean turnOffBlueTooth() {
        if (isBleEnable()){
            return bluetoothAdapter.disable();
        }
        return true;
    }

    /**
     * clear bluetooth cache
     * @param address 蓝牙设备地址
     * @return 是否清理成功
     */
    public boolean refreshDeviceCache(String address) {
        if (bleRequestImpl != null) {
            return bleRequestImpl.refreshDeviceCache(address);
        }
        return false;
    }

    public boolean isDeviceBusy(T device){
        if (bleRequestImpl != null) {
            return bleRequestImpl.isDeviceBusy(device);
        }
        return false;
    }

    public static Options options(){
        if(options == null){
            options = new Options();
        }
        return options;
    }

    public Context getContext(){
        return context;
    }

}
