package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGatt;
import androidx.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.queue.reconnect.DefaultReConnectHandler;
import cn.com.heaton.blelibrary.ble.queue.retry.RetryDispatcher;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;

@Implement(ConnectRequest.class)
public class ConnectRequest<T extends BleDevice> implements ConnectWrapperCallback<T> {

    private static final String TAG = "ConnectRequest";
    private final Map<String, T> devices = new HashMap<>();
    private final Map<String, T> connectedDevices = new HashMap<>();
    private final BleConnectsDispatcher<T> dispatcher = new BleConnectsDispatcher<>();
    private final BleRequestImpl<T> bleRequest = BleRequestImpl.getBleRequest();
    private BleConnectCallback<T> connectCallback;
    private final List<BleConnectCallback<T>> connectInnerCallbacks = new ArrayList<>();
    private final BleWrapperCallback<T> bleWrapperCallback = Ble.options().getBleWrapperCallback();

    protected ConnectRequest() {
        DefaultReConnectHandler<T> connectHandler = DefaultReConnectHandler.provideReconnectHandler();
        addConnectHandlerCallbacks(connectHandler);
        RetryDispatcher<T> retryDispatcher = RetryDispatcher.getInstance();
        addConnectHandlerCallbacks(retryDispatcher);
    }

    public boolean connect(T device){
        return connect(device, connectCallback);
    }

    public synchronized boolean connect(T device, BleConnectCallback<T> callback) {
        connectCallback = callback;
        if (device == null){
            doConnectException(null, BleStates.DeviceNull);
            return false;
        }
        if (device.isConnecting()){
            return false;
        }
        if (!Ble.getInstance().isBleEnable()){
            doConnectException(device, BleStates.BluetoothNotOpen);
            return false;
        }
        if (connectedDevices.size() >= Ble.options().getMaxConnectNum()){
            BleLog.e(TAG, "Maximum number of connections Exception");
            doConnectException(device, BleStates.MaxConnectNumException);
            return false;
        }
        device.setAutoConnect(Ble.options().autoConnect);
        addBleToPool(device);
        return bleRequest.connect(device);
    }

    private void doConnectException(final T device, final int errorCode){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectCallback != null){
                    connectCallback.onConnectFailed(device, errorCode);
                }
            }
        });
        for (BleConnectCallback<T> callback: connectInnerCallbacks) {
            callback.onConnectFailed(device, errorCode);
        }
    }

    public boolean connect(String address, BleConnectCallback<T> callback) {
        T bleDevice = (T) Ble.options().getFactory().create(address, "");
        return connect(bleDevice, callback);
    }

    /**
     * 连接多个设备
     * @param devices
     * @param callback
     */
    public void connect(List<T> devices, final BleConnectCallback<T> callback) {
        dispatcher.excute(devices, new BleConnectsDispatcher.NextCallback<T>() {
            @Override
            public void onNext(T device) {
                connect(device, callback);
            }
        });
    }

    /**
     * 取消正在连接的设备
     * @param device
     */
    public void cancelConnecting(T device) {
        boolean connecting = device.isConnecting();
        boolean ready_connect = dispatcher.isContains(device);
        if (connecting || ready_connect){
            if (connectCallback != null){
                BleLog.d(TAG, "cancel connecting device："+device.getBleName());
                connectCallback.onConnectCancel(device);
            }
            if (connecting){
                disconnect(device);
                bleRequest.cancelTimeout(device.getBleAddress());
                device.setConnectionState(BleDevice.DISCONNECT);
                onConnectionChanged(device);
            }
            if (ready_connect){
                dispatcher.cancelOne(device);
            }
        }
    }

    public void cancelConnectings(List<T> devices){
        for (T device: devices){
            cancelConnecting(device);
        }
    }

    /**
     * 通过蓝牙地址断开设备
     * @param address 蓝牙地址
     */
    public void disconnect(String address){
        if (connectedDevices.containsKey(address)){
            disconnect(connectedDevices.get(address));
        }
    }

    /**
     * 无回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device) {
        disconnect(device, connectCallback);
    }

    /**
     * 带回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device, BleConnectCallback<T> callback) {
        if (device != null){
            connectCallback = callback;
            device.setAutoConnect(false);
            bleRequest.disconnect(device.getBleAddress());
        }
    }

    /**
     * 兼容原生android系统直接断开系统蓝牙导致的异常
     * 直接断开系统蓝牙不回调onConnectionStateChange接口问题
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void closeBluetooth(){
        if (!connectedDevices.isEmpty()){
            for (T device: connectedDevices.values()) {
                if (connectCallback != null){
                    device.setConnectionState(BleDevice.DISCONNECT);
                    BleLog.e(TAG, "System Bluetooth is disconnected>>>> "+device.getBleName());
                    connectCallback.onConnectionChanged(device);
                }
            }
            bleRequest.close();
            connectedDevices.clear();
            devices.clear();
        }
    }

    private void runOnUiThread(Runnable runnable){
        ThreadUtils.ui(runnable);
    }

    void addConnectHandlerCallbacks(BleConnectCallback<T> callback){
        connectInnerCallbacks.add(callback);
    }

    @Override
    public void onConnectionChanged(final T bleDevice) {
        if (bleDevice == null)return;
        if (bleDevice.isConnected()){
            connectedDevices.put(bleDevice.getBleAddress(), bleDevice);
            BleLog.d(TAG, "connected>>>> "+bleDevice.getBleName());
        }else if(bleDevice.isDisconnected()) {
            connectedDevices.remove(bleDevice.getBleAddress());
            devices.remove(bleDevice.getBleAddress());
            BleLog.d(TAG, "disconnected>>>> "+bleDevice.getBleName());
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectCallback != null){
                    connectCallback.onConnectionChanged(bleDevice);
                }
                if (bleWrapperCallback != null){
                    bleWrapperCallback.onConnectionChanged(bleDevice);
                }
            }
        });

        for (BleConnectCallback<T> callback: connectInnerCallbacks) {
            callback.onConnectionChanged(bleDevice);
        }

    }

    @Override
    public void onConnectFailed(final T bleDevice, final int errorCode) {
        if (bleDevice == null)return;
        BleLog.e(TAG, "onConnectFailed>>>> "+bleDevice.getBleName()+"\n异常码:"+errorCode);
        bleDevice.setConnectionState(BleDevice.DISCONNECT);
        onConnectionChanged(bleDevice);
        doConnectException(bleDevice, errorCode);
    }

    @Override
    public void onReady(final T bleDevice) {
        if (bleDevice == null)return;
        BleLog.d(TAG, "onReady>>>> "+bleDevice.getBleName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (connectCallback != null){
                    connectCallback.onReady(bleDevice);
                }
                if (bleWrapperCallback != null){
                    bleWrapperCallback.onReady(bleDevice);
                }
            }
        });
    }

    @Override
    public void onServicesDiscovered(final T device, BluetoothGatt gatt) {
        BleLog.d(TAG, "onServicesDiscovered>>>> "+device.getBleName());
        if (connectCallback != null){
            connectCallback.onServicesDiscovered(device, gatt);
        }
        if (bleWrapperCallback != null){
            bleWrapperCallback.onServicesDiscovered(device, gatt);
        }
    }

    private void addBleToPool(T device) {
        if (devices.containsKey(device.getBleAddress())){
            BleLog.d(TAG, "addBleToPool>>>> device pool already exist device");
            return;
        }
        devices.put(device.getBleAddress(), device);
        BleLog.d(TAG, "addBleToPool>>>> added a new device to the device pool");
    }

    public T getBleDevice(String address) {
        if(TextUtils.isEmpty(address)){
            BleLog.e(TAG,"By address to get BleDevice but address is null");
            return null;
        }
        return devices.get(address);
    }

    /**
     *
     * @return 已经连接的蓝牙设备集合
     */
    public ArrayList<T> getConnectedDevices() {
        return new ArrayList<>(connectedDevices.values());
    }

    public void cancelConnectCallback(){
        connectCallback = null;
    }
}
