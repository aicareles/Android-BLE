package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGatt;
import android.support.annotation.RestrictTo;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.queue.reconnect.ReconnectHandlerCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.queue.reconnect.DefaultReConnectHandler;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;

@Implement(ConnectRequest.class)
public class ConnectRequest<T extends BleDevice> implements ConnectWrapperCallback<T> {

    private static final String TAG = "ConnectRequest";
    private BleConnectCallback<T> connectCallback;
    private final ArrayList<T> devices = new ArrayList<>();
    private final ArrayList<T> connectedDevices = new ArrayList<>();
    private final BleConnectsDispatcher<T> dispatcher = new BleConnectsDispatcher<>();
    private final BleRequestImpl<T> bleRequest = BleRequestImpl.getBleRequest();
    private final ReconnectHandlerCallback<T> reconnectHandlerCallback = DefaultReConnectHandler.provideReconnectHandler();
    private final BleWrapperCallback<T> bleWrapperCallback;

    protected ConnectRequest() {
        bleWrapperCallback = Ble.options().bleWrapperCallback;
    }

    public boolean connect(T device){
        return connect(device, connectCallback);
    }

    public boolean connect(T device, BleConnectCallback<T> callback) {
        connectCallback = callback;
        if (device == null){
            doConnectException(device, BleStates.DeviceNull);
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
                    connectCallback.onConnectException(device, errorCode);
                }
            }
        });
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
            if (null != connectCallback){
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
        for (T bleDevice : connectedDevices) {
            if (bleDevice.getBleAddress().equals(address)) {
                disconnect(bleDevice);
            }
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
            for (T device: connectedDevices) {
                if (null != connectCallback){
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

    @Override
    public void onConnectionChanged(final T bleDevice) {
        if (bleDevice == null)return;
        if (bleDevice.isConnected()){
            connectedDevices.add(bleDevice);
            BleLog.d(TAG, "connected>>>> "+bleDevice.getBleName());
        }else if(bleDevice.isDisconnected()) {
            connectedDevices.remove(bleDevice);
            devices.remove(bleDevice);
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
                reconnectHandlerCallback.onConnectionChanged(bleDevice);
            }
        });

    }

    @Override
    public void onConnectException(final T bleDevice, final int errorCode) {
        if (bleDevice == null)return;
        BleLog.e(TAG, "ConnectException>>>> "+bleDevice.getBleName()+"\n异常码:"+errorCode);
        doConnectException(bleDevice, errorCode);
    }

    @Override
    public void onConnectTimeOut(final T bleDevice) {
        if (bleDevice == null)return;
        BleLog.e(TAG, "ConnectTimeOut>>>> "+bleDevice.getBleName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
                    connectCallback.onConnectTimeOut(bleDevice);
                }
            }
        });
        bleDevice.setConnectionState(BleDevice.DISCONNECT);
        onConnectionChanged(bleDevice);
    }

    @Override
    public void onReady(final T bleDevice) {
        if (bleDevice == null)return;
        BleLog.d(TAG, "onReady>>>> "+bleDevice.getBleName());
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
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
        if (null != connectCallback){
            connectCallback.onServicesDiscovered(device, gatt);
        }

        if (bleWrapperCallback != null){
            bleWrapperCallback.onServicesDiscovered(device, gatt);
        }
    }

    private void addBleToPool(T device) {
        for (T bleDevice : devices){
            if(bleDevice.getBleAddress().equals(device.getBleAddress())){
                BleLog.d(TAG, "addBleToPool>>>> device pool already exist device");
                return;
            }
        }
        devices.add(device);
        BleLog.d(TAG, "addBleToPool>>>> added a new device to the device pool");
    }

    public T getBleDevice(String address) {
        if(TextUtils.isEmpty(address)){
            BleLog.e(TAG,"By address to get BleDevice but address is null");
            return null;
        }
        synchronized (devices){
            if(devices.size() > 0){
                for (T bleDevice : devices){
                    if(bleDevice.getBleAddress().equals(address)){
                        return bleDevice;
                    }
                }
            }
            BleLog.e(TAG,"By address to get BleDevice and BleDevice isn't exist");
            return null;
        }

    }

    /**
     *
     * @return 已经连接的蓝牙设备集合
     */

    public ArrayList<T> getConnectedDevices() {
        return connectedDevices;
    }

    public void cancelConnectCallback(){
        connectCallback = null;
    }
}
