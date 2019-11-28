package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.queue.ConnectQueue;
import cn.com.heaton.blelibrary.ble.queue.RequestTask;
import cn.com.heaton.blelibrary.ble.utils.TaskExecutor;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */
@Implement(ConnectRequest.class)
public class ConnectRequest<T extends BleDevice> implements ConnectWrapperCallback {

    private static final String TAG = "ConnectRequest";
    private static final long DEFALUT_CONNECT_DELAY = 2000L;
    private BleConnectCallback<T> connectCallback;
    private ArrayList<T> devices = new ArrayList<>();
    private ArrayList<T> connetedDevices = new ArrayList<>();
    private ArrayList<T> autoDevices = new ArrayList<>();
    private BleConnectTask<T> task = new BleConnectTask<>();

    protected ConnectRequest() {}

    public boolean reconnect(String address){
        for (T device : autoDevices) {
            if (TextUtils.equals(address, device.getBleAddress())){
                return connect(device, connectCallback);
            }
        }
        return false;
    }

    public boolean connect(T device, BleConnectCallback<T> callback) {
        addBleDevice(device);
        connectCallback = callback;
        boolean result = false;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (bleRequest != null) {
            result = bleRequest.connect(device.getBleAddress());
        }
        return result;
    }

    public boolean connect(String address, BleConnectCallback<T> callback) {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            BleLog.e(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothDevice device = adapter.getRemoteDevice(address);
        T bleDevice = BleFactory.create(device);
        return connect(bleDevice, callback);
    }

    /**
     * 连接多个设备
     * @param devices
     * @param callback
     */
    public void connect(List<T> devices, final BleConnectCallback<T> callback) {
        final BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (bleRequest != null) {
            task.excute(devices, new BleConnectTask.NextCallback<T>() {
                @Override
                public void onNext(T device) {
                    connect(device, callback);
                }
            });
        }
    }

    /**
     * 取消正在连接的设备
     * @param device
     */
    public void cancelConnectting(T device) {
        boolean connectting = device.isConnectting();
        boolean ready_connect = task.isContains(device);
        if (connectting || ready_connect){
            if (null != connectCallback){
                connectCallback.onConnectCancel(device);
            }
            if (connectting){
                disconnect(device.getBleAddress());
                final BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
                bleRequest.cancelTimeout(device.getBleAddress());
                device.setConnectionState(BleStates.BleStatus.DISCONNECT);
                connectCallback.onConnectionChanged(device);
            }
            if (ready_connect){
                task.cancelOne(device);
            }
        }
    }

    public void cancelConnecttings(List<T> devices){
        for (T device: devices){
            cancelConnectting(device);
        }
    }

    /**
     * 通过蓝牙地址断开设备
     * @param address 蓝牙地址
     */
    public void disconnect(String address){
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (bleRequest != null) {
            //Traverse the connected device collection to disconnect automatically cancel the automatic connection
            ArrayList<T> connetedDevices = getConnetedDevices();
            for (T bleDevice : connetedDevices) {
                if (bleDevice.getBleAddress().equals(address)) {
                    bleDevice.setAutoConnect(false);
                }
            }
            bleRequest.disconnect(address);
        }
    }

    /**
     * 无回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device) {
        if (device != null){
            disconnect(device.getBleAddress());
        }
    }

    /**
     * 带回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device, BleConnectCallback<T> callback) {
        if (device != null){
            disconnect(device.getBleAddress());
            connectCallback = callback;
        }
    }

    @Override
    public void onConnectionChanged(BluetoothDevice device, int status) {
        final T bleDevice = getBleDevice(device);
        if (bleDevice == null)return;
        bleDevice.setConnectionState(status);
        if (status == BleStates.BleStatus.CONNECTED){
            connetedDevices.add(bleDevice);
            BleLog.e(TAG, "CONNECTED>>>> "+bleDevice.getBleName());
            //After the success of the connection can be considered automatically reconnect
            //If it is automatically connected device is removed from the automatic connection pool
            removeAutoPool(bleDevice);
        }else if(status == BleStates.BleStatus.DISCONNECT) {
            connetedDevices.remove(bleDevice);
            devices.remove(bleDevice);
            BleLog.e(TAG, "DISCONNECT>>>> "+bleDevice.getBleName());
            addAutoPool(bleDevice);
        }
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
                    connectCallback.onConnectionChanged(bleDevice);
                }
            }
        });
    }

    @Override
    public void onConnectException(BluetoothDevice device) {
        final T bleDevice = getBleDevice(device);
        if (bleDevice == null)return;
        final int errorCode;
        if (bleDevice.isConnected()) {//Mcu connection is broken or the signal is weak and other reasons disconnect
            errorCode = BleStates.BleStatus.ConnectException;
        } else if (bleDevice.isConnectting()) {//Connection failed
            errorCode = BleStates.BleStatus.ConnectFailed;
        } else {//Abnormal state (in theory, there is no such situation)
            errorCode = BleStates.BleStatus.ConnectError;
        }
        BleLog.e(TAG, "ConnectException>>>> "+bleDevice.getBleName()+"\n异常码:"+errorCode);
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
                    connectCallback.onConnectException(bleDevice, errorCode);
                }
            }
        });
    }

    @Override
    public void onConnectTimeOut(BluetoothDevice device) {
        final T bleDevice = getBleDevice(device);
        if (bleDevice == null)return;
        BleLog.e(TAG, "ConnectTimeOut>>>> "+bleDevice.getBleName());
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
                    connectCallback.onConnectTimeOut(bleDevice);
                }
            }
        });
        onConnectionChanged(device, BleStates.BleStatus.DISCONNECT);
    }

    @Override
    public void onReady(BluetoothDevice device) {
        final T bleDevice = getBleDevice(device);
        if (bleDevice == null)return;
        BleLog.e(TAG, "onReady>>>> "+bleDevice.getBleName());
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                if (null != connectCallback){
                    connectCallback.onReady(bleDevice);
                }
            }
        });
    }

    @Override
    public void onServicesDiscovered(final BluetoothDevice device) {
        BleLog.e(TAG, "onServicesDiscovered>>>> "+device.getName());
        if (null != connectCallback){
            TaskExecutor.mainThread(new Runnable() {
                @Override
                public void run() {
                    if (null != connectCallback){
                        connectCallback.onServicesDiscovered(getBleDevice(device));
                    }
                }
            });
        }
    }

    private void addBleDevice(T device) {
        if (device == null)throw new IllegalArgumentException("device is not null");
        if (getBleDevice(device.getBleAddress()) == null) {
            devices.add(device);
            BleLog.i(TAG, "addBleDevice>>>> Added a device to the device pool");
        }
    }

    public T getBleDevice(int index) {
        return devices.get(index);
    }

    public T getBleDevice(String address) {
        if(TextUtils.isEmpty(address)){
            BleLog.w(TAG,"By address to get BleDevice but address is null");
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
            BleLog.w(TAG,"By address to get BleDevice and BleDevice isn't exist");
            return null;
        }

    }

    /**
     * 获取蓝牙对象
     *
     * @param device 原生蓝牙对象
     * @return 蓝牙对象
     */
    public T getBleDevice(BluetoothDevice device) {
        if (device == null) {
            BleLog.w(TAG, "By BluetoothDevice to get BleDevice but BluetoothDevice is null");
            return null;
        }
        return getBleDevice(device.getAddress());
    }

    /**
     *
     * @return 已经连接的蓝牙设备集合
     */

    public ArrayList<T> getConnetedDevices() {
        return connetedDevices;
    }

    /**
     * If it is automatically connected device is removed from the automatic connection pool
     *
     * @param device Device object
     */
    private void removeAutoPool(BleDevice device) {
        if (device == null) return;
        Iterator<T> iterator = autoDevices.iterator();
        while (iterator.hasNext()) {
            BleDevice item = iterator.next();
            if (device.getBleAddress().equals(item.getBleAddress())) {
                iterator.remove();
            }
        }
    }

    /**
     * Add a disconnected device to the autouppool
     *
     * @param device Device object
     */
    private void addAutoPool(T device) {
        if (device == null) return;
        if (device.isAutoConnect()) {
            BleLog.w(TAG, "addAutoPool: "+"Add automatic connection device to the connection pool");
            autoDevices.add(device);
            ConnectQueue.getInstance().put(DEFALUT_CONNECT_DELAY, RequestTask.newConnectTask(device.getBleAddress()));
        }
    }

    public void resetReConnect(T device, boolean autoConnect){
        if (device == null)return;
        device.setAutoConnect(autoConnect);
        if (!autoConnect){
            removeAutoPool(device);
            if (device.isConnectting()){
                disconnect(device);
            }
        }else {//重连
            addAutoPool(device);
        }
    }
}
