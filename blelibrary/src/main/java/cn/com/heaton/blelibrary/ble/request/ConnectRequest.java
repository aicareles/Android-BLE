package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */

public class ConnectRequest<T extends BleDevice> implements BleHandler.ReceiveMessage {

    private static final String TAG = "ConnectRequest";
    private BleFactory<T> mBleFactory;
    private BleConnCallback<T> mBleLisenter;
    private BleHandler mHandler;

    private ArrayList<T> mDevices = new ArrayList<>();
    private ArrayList<T> mConnetedDevices = new ArrayList<>();

    private static volatile ConnectRequest instance;

    public static <T extends BleDevice> ConnectRequest<T> getInstance() {
        if (instance == null) {
            synchronized (ConnectRequest.class) {
                if (instance == null) {
                    instance = new ConnectRequest();
                }
            }
        }
        return instance;
    }

    private ConnectRequest() {
        mBleFactory = new BleFactory<T>();
        mHandler = BleHandler.getHandler();
        mHandler.setHandlerCallback(this);
    }

    public boolean connect(T device, BleConnCallback<T> lisenter) {
        if (!addBleDevice(device)) {
            return false;
        }
        this.mBleLisenter = lisenter;
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            result = service.connect(device.getBleAddress());
        }
        return result;
    }

    public void disconnect(BleDevice device) {
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (service != null) {
            service.disconnect(device.getBleAddress());
        }
    }

    @Override
    public void handleMessage(Message msg) {
        BleLog.e(TAG, "handleMessage: "+msg.arg1);
        T t = null;
        if (msg.obj instanceof BluetoothDevice) {
            t = getBleDevice((BluetoothDevice) msg.obj);
        }
        if (t == null) return;
        switch (msg.what) {
            case BleStates.BleStatus.ConnectException:
                int errorCode = msg.arg1;
                //Disconnect and clear the connection
                mBleLisenter.onConnectException(t, errorCode);
                mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 0, 0, msg.obj).sendToTarget();
                break;
            case BleStates.BleStatus.ConnectionChanged:
                if (msg.arg1 == 1) {
                    //connect
                    t.setConnectionState(BleStates.BleStatus.CONNECTED);
                    mConnetedDevices.add(t);
                    BleLog.e(TAG, "handleMessage:++++CONNECTED ");
//                        //After the success of the connection can be considered automatically reconnect
//                        device.setAutoConnect(true);
//                        //If it is automatically connected device is removed from the automatic connection pool
//                        removeAutoPool(device);
                } else if (msg.arg1 == 0) {
                    //disconnect
                    t.setConnectionState(BleStates.BleStatus.DISCONNECT);
                    mConnetedDevices.remove(t);
                    mDevices.remove(t);
                    BleLog.e(TAG, "handleMessage:++++DISCONNECT ");
////                    Log.i(TAG, "mDevices quantity: " + mDevices.size());
//                    addAutoPool(device);
                } else if (msg.arg1 == 2) {
                    //connectting
                    t.setConnectionState(BleStates.BleStatus.CONNECTING);
                }
                mBleLisenter.onConnectionChanged(t);
                break;
            default:
                break;
        }
    }

    private boolean addBleDevice(T device) {
        if (device == null || mDevices.contains(device)) {
            BleLog.i(TAG, "addBleDevice" + "Already contains the device");
            return false;
        }
        mDevices.add(device);
        BleLog.i(TAG, "addBleDevice" + "Added a device to the device pool");
        return true;
    }

    public T getBleDevice(int index) {
        return mDevices.get(index);
    }

    /**
     * get BLE
     *
     * @param device blutoothdevice
     * @return bleDeive
     */
    public T getBleDevice(BluetoothDevice device) {
        if (device == null) {
            BleLog.w(TAG, "getBleDevice: " + "device is null");
            return null;
        }
        synchronized (mDevices) {
            if (mDevices.size() > 0) {
                for (T bleDevice : mDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        BleLog.w(TAG, "getBleDevice: " + "device is exist");
                        return bleDevice;
                    }
                }
            }
            T newDevice = (T) BleFactory.create(BleDevice.class, Ble.getInstance(), device);
            BleLog.w(TAG, "getBleDevice: " + "device is new");
            return newDevice;
        }
    }

    /**
     * Gets the connected device
     *
     * @return connected device
     */

    public ArrayList<T> getConnetedDevices() {
        return mConnetedDevices;
    }


}
