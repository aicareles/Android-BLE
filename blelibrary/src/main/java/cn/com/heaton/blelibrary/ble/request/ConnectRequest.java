package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.os.SystemClock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperLisenter;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.utils.TaskExecutor;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */
@Implement(ConnectRequest.class)
public class ConnectRequest<T extends BleDevice> implements ConnectWrapperLisenter{

    private static final String TAG = "ConnectRequest";
    private List<BleConnectCallback<T>> mConnectCallbacks = new ArrayList<>();
    private ArrayList<T> mDevices = new ArrayList<>();
    private ArrayList<T> mConnetedDevices = new ArrayList<>();
    private ArrayList<T> mAutoDevices = new ArrayList<>();
    private final byte[] lock = new byte[1];
    private AutoConThread autoConThread;
    private Ble<T> mBle;

    protected ConnectRequest() {
        mBle = Ble.getInstance();
        autoConThread = new AutoConThread();
        autoConThread.start();
    }

    public boolean connect(T device, BleConnectCallback<T> lisenter) {
        addBleDevice(device);
        if(lisenter != null && !mConnectCallbacks.contains(lisenter)){
            this.mConnectCallbacks.add(lisenter);
        }
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            device.setAutoConnect(device.isAutoConnect());
            result = service.connect(device.getBleAddress());
        }
        return result;
    }

    public boolean connect(String address, BleConnectCallback<T> lisenter) {
        //check if address is vaild. if don't check it, getRemoteDevice(adress)
        //will throw exception when the address is invalid
        boolean isValidAddress = BluetoothAdapter.checkBluetoothAddress(address);
        if (!isValidAddress) {
            L.d(TAG, "the device address is invalid");
            return false;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return false;
        }
        BluetoothDevice device = adapter.getRemoteDevice(address);
//        T bleDevice = (T) BleFactory.create(BleDevice.class, device);
        T bleDevice = (T) new BleDevice(device);
        return connect(bleDevice, lisenter);
    }

    /**
     * 通过蓝牙地址断开设备
     * @param address 蓝牙地址
     */
    public void disconnect(String address){
        boolean isValidAddress = BluetoothAdapter.checkBluetoothAddress(address);
        if (!isValidAddress) {
            L.d(TAG, "the device address is invalid");
            return;
        }
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return;
        }
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            //Traverse the connected device collection to disconnect automatically cancel the automatic connection
            ArrayList<T> connetedDevices = getConnetedDevices();
            for (T bleDevice : connetedDevices) {
                if (bleDevice.getBleAddress().equals(address)) {
                    bleDevice.setAutoConnect(false);
                }
            }
            service.disconnect(address);
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
    public void disconnect(BleDevice device, BleConnectCallback<T> lisenter) {
        if (device != null){
            disconnect(device.getBleAddress());
            if(lisenter != null && !mConnectCallbacks.contains(lisenter)){
                this.mConnectCallbacks.add(lisenter);
            }
        }
    }

    @Override
    public void onConnectionChanged(BluetoothDevice device, int status) {
        final T d = getBleDevice(device);
        if (d == null)return;
        d.setConnectionState(status);
        if (status == BleStates.BleStatus.CONNECTED){
            mConnetedDevices.add(d);
            L.e(TAG, "CONNECTED>>>> "+d.getBleName());
            //After the success of the connection can be considered automatically reconnect
            //If it is automatically connected device is removed from the automatic connection pool
            removeAutoPool(d);
        }else if(status == BleStates.BleStatus.DISCONNECT) {
            mConnetedDevices.remove(d);
            mDevices.remove(d);
            L.e(TAG, "DISCONNECT>>>> "+d.getBleName());
            //移除通知
            Ble.getInstance().cancelNotify(d);
            addAutoPool(d);
            autoConThread.interrupt();
        }
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                for (BleConnectCallback<T> callback : mConnectCallbacks){
                    callback.onConnectionChanged(d);
                }
            }
        });
    }

    @Override
    public void onConnectException(BluetoothDevice device) {
        final T d = getBleDevice(device);
        if (d == null)return;
        final int errorCode;
        if (d.isConnected()) {//Mcu connection is broken or the signal is weak and other reasons disconnect
            errorCode = BleStates.BleStatus.ConnectException;
        } else if (d.isConnectting()) {//Connection failed
            errorCode = BleStates.BleStatus.ConnectFailed;
        } else {//Abnormal state (in theory, there is no such situation)
            errorCode = BleStates.BleStatus.ConnectError;
        }
        L.e(TAG, "ConnectException>>>> "+d.getBleName()+"\n异常码:"+errorCode);
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                for (BleConnectCallback<T> callback : mConnectCallbacks){
                    callback.onConnectException(d, errorCode);
                }
            }
        });
    }

    @Override
    public void onConnectTimeOut(BluetoothDevice device) {
        final T d = getBleDevice(device);
        if (d == null)return;
        L.e(TAG, "ConnectTimeOut>>>> "+d.getBleName());
        TaskExecutor.mainThread(new Runnable() {
            @Override
            public void run() {
                for (BleConnectCallback<T> callback : mConnectCallbacks){
                    callback.onConnectTimeOut(d);
                }
            }
        });
        onConnectionChanged(device, BleStates.BleStatus.DISCONNECT);
    }

    private boolean addBleDevice(T device) {
        if (device == null)throw new IllegalArgumentException("device is not null");
        if (getBleDevice(device.getBleAddress()) != null) {
            L.i(TAG, "addBleDevice>>>> Already contains the device");
            return true;
        }
        mDevices.add(device);
        L.i(TAG, "addBleDevice>>>> Added a device to the device pool");
        return true;
    }

    public T getBleDevice(int index) {
        return mDevices.get(index);
    }

    public T getBleDevice(String address) {
        if(address == null){
            L.w(TAG,"By address to get BleDevice but address is null");
            return null;
        }
        synchronized (mDevices){
            if(mDevices.size() > 0){
                for (T bleDevice : mDevices){
                    if(bleDevice.getBleAddress().equals(address)){
                        return bleDevice;
                    }
                }
            }
            L.w(TAG,"By address to get BleDevice and BleDevice isn't exist");
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
            L.w(TAG, "By BluetoothDevice to get BleDevice but BluetoothDevice is null");
            return null;
        }
        return getBleDevice(device.getAddress());
    }

    /**
     *
     * @return 已经连接的蓝牙设备集合
     */

    public ArrayList<T> getConnetedDevices() {
        return mConnetedDevices;
    }

    private class AutoConThread extends Thread {
        @Override
        public void run() {
            while (Ble.options().autoConnect) {
                synchronized (lock){
                    if (mAutoDevices.size() > 0) {
                        autoConnect();
                        try {
                            Thread.sleep(Ble.options().connectTimeout);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

    }

    /**
     * If it is automatically connected device is removed from the automatic connection pool
     *
     * @param device Device object
     */
    public void removeAutoPool(BleDevice device) {
        if (device == null) return;
        Iterator<T> iterator = mAutoDevices.iterator();
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
    public void addAutoPool(T device) {
        if (device == null) return;
        for (BleDevice item : mAutoDevices) {
            if (device.getBleAddress().equals(item.getBleAddress())) {
                L.w(TAG,"自动连接池中已存在");
                return;
            }
        }
        if (device.isAutoConnect()) {
            L.w(TAG, "addAutoPool: "+"Add automatic connection device to the connection pool");
            mAutoDevices.add(device);
        }
    }

    private void autoConnect() {
        TaskExecutor.executeTask(new Runnable() {
            @Override
            public void run() {
                synchronized (mBle.getLocker()) {
                    for (int i=0; i<mAutoDevices.size(); i++) {
                        final T autoDevice = mAutoDevices.get(i);
                        if (autoDevice.getConnectionState() == BleStates.BleStatus.DISCONNECT && autoDevice.isAutoConnect()) {
                            L.e(TAG, "onLeScan: 正在重连设备>>>>..."+autoDevice.getBleName());
                            mBle.reconnect(autoDevice);
                            SystemClock.sleep(2000L);
                        }
                    }
                }
            }
        });
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
