package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.TaskExecutor;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperLisenter;
import cn.com.heaton.blelibrary.ble.factory.BleFactory;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */
@Implement(ConnectRequest.class)
public class ConnectRequest<T extends BleDevice> implements ConnectWrapperLisenter {

    private static final String TAG = "ConnectRequest";
    private List<BleConnectCallback<T>> mConnectCallbacks = new ArrayList<>();
    private ArrayList<T> mDevices = new ArrayList<>();
    private ArrayList<T> mConnetedDevices = new ArrayList<>();

    protected ConnectRequest() {}

    public boolean connect(T device, BleConnectCallback<T> lisenter) {
        if (!addBleDevice(device)) {
            return false;
        }
        if(lisenter != null && !mConnectCallbacks.contains(lisenter)){
            this.mConnectCallbacks.add(lisenter);
        }
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
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
        T bleDevice = (T) BleFactory.create(BleDevice.class, device);
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
            service.disconnect(address);
        }
    }

    /**
     * 无回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device) {
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            service.disconnect(device.getBleAddress());
        }
    }

    /**
     * 带回调的断开
     * @param device 设备对象
     */
    public void disconnect(BleDevice device, BleConnectCallback<T> lisenter) {
        if(!mConnectCallbacks.contains(lisenter)){
            this.mConnectCallbacks.add(lisenter);
        }
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            service.disconnect(device.getBleAddress());
        }
    }

    @Override
    public void onConnectionChanged(BluetoothDevice device, int status) {
        final T d = getBleDevice(device);
        d.setConnectionState(status);
        if (status == BleStates.BleStatus.CONNECTED){
            mConnetedDevices.add(d);
            L.e(TAG, "handleMessage:++++CONNECTED "+d.getBleName());
            //After the success of the connection can be considered automatically reconnect
            //If it is automatically connected device is removed from the automatic connection pool
            Ble.getInstance().removeAutoPool(d);
        }else if(status == BleStates.BleStatus.DISCONNECT) {
            mConnetedDevices.remove(d);
            mDevices.remove(d);
            L.e(TAG, "handleMessage:++++DISCONNECT "+d.getBleName());
            //移除通知
            Ble.getInstance().cancelNotify(d);
            Ble.getInstance().addAutoPool(d);
        }
        TaskExecutor.runOnUIThread(new Runnable() {
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
        final int errorCode;
        if (d.isConnected()) {
            //Mcu connection is broken or the signal is weak and other reasons disconnect
            errorCode = BleStates.BleStatus.ConnectException;
        } else if (d.isConnectting()) {
            //Connection failed
            errorCode = BleStates.BleStatus.ConnectFailed;
        } else {
            //Abnormal state (in theory, there is no such situation)
            errorCode = BleStates.BleStatus.ConnectError;
        }
        TaskExecutor.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (BleConnectCallback<T> callback : mConnectCallbacks){
                    callback.onConnectException(d, errorCode);
                }
            }
        });
        onConnectionChanged(device, BleStates.BleStatus.DISCONNECT);
    }

    @Override
    public void onConnectTimeOut(BluetoothDevice device) {
        final T d = getBleDevice(device);
        TaskExecutor.runOnUIThread(new Runnable() {
            @Override
            public void run() {
                for (BleConnectCallback<T> callback : mConnectCallbacks){
                    callback.onConnectTimeOut(d);
                }
            }
        });
    }

    private boolean addBleDevice(T device) {
        if (device == null || mDevices.contains(device)) {
            L.i(TAG, "addBleDevice" + "Already contains the device");
            return false;
        }
        mDevices.add(device);
        L.i(TAG, "addBleDevice" + "Added a device to the device pool");
        return true;
    }

    //自动连接相关   暂时屏蔽
    /*private void addBleDevice1(T device) {
        if (device == null || mDevices.contains(device)) {
            L.i(TAG, "addBleDevice" + "Already contains the device");
        }else {
            mDevices.add(device);
            L.i(TAG, "addBleDevice" + "Added a device to the device pool");
        }
    }*/

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
                        L.e(TAG,"By address to get BleDevice and BleDevice is exist");
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
        synchronized (mDevices) {
            if (mDevices.size() > 0) {
                for (T bleDevice : mDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        L.e(TAG, "By BluetoothDevice to get BleDevice and device is exist");
                        return bleDevice;
                    }
                }
            }
//            T newDevice = (T) BleFactory.create(BleDevice.class, Ble.getInstance(), device);
//            L.e(TAG, "By BluetoothDevice to get BleDevice and device is new");
            L.e(TAG, "By BluetoothDevice to get BleDevice and isn't exist");
            return null;
        }
    }

    /**
     *
     * @return 已经连接的蓝牙设备集合
     */

    public ArrayList<T> getConnetedDevices() {
        return mConnetedDevices;
    }
}
