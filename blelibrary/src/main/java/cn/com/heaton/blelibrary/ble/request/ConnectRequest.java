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

public class ConnectRequest<T> implements BleHandler.ReceiveMessage {

    private static final String TAG = "ConnectRequest";
    private BleFactory mBleFactory;
    private BleConnCallback<BleDevice> mBleLisenter;
    private BleHandler mHandler;

    private ArrayList<BleDevice> mDevices = new ArrayList<>();
    private ArrayList<BleDevice> mConnetedDevices = new ArrayList<>();

    private static volatile ConnectRequest instance;

    public static ConnectRequest getInstance() {
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
        mBleFactory = new BleFactory();
        mHandler = BleHandler.getHandler();
        mHandler.setHandlerCallback(this);
    }

    public boolean connect(BleDevice device, BleConnCallback<BleDevice> lisenter) {
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
        BleDevice t = null;
        if (msg.obj instanceof BluetoothDevice) {
            t = mBleFactory.create(Ble.getInstance(), (BluetoothDevice) msg.obj);
        }
        if (t == null) return;

        switch (msg.what) {
            case BleStates.BleStatus.ConnectException:
                int errorCode = msg.arg1;
                //断开并清除连接
                mBleLisenter.onConnectException(t, errorCode);
                mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 0, 0, msg.obj).sendToTarget();
                break;
            case BleStates.BleStatus.ConnectionChanged:
                if (msg.arg1 == 1) {
                    //connect
                    t.setConnectionState(BleStates.BleStatus.CONNECTED);
                    mConnetedDevices.add(t);
                    BleLog.e(TAG, "handleMessage:++++CONNECTED ");
//                        //连接成功后 才能被认为可以自动重连
//                        device.setAutoConnect(true);
//                        //如果是自动连接的设备  则从自动连接池中移除
//                        removeAutoPool(device);
                } else if (msg.arg1 == 0) {
                    //disconnect
                    t.setConnectionState(BleStates.BleStatus.DISCONNECT);
                    mConnetedDevices.remove(t);
                    mDevices.remove(t);
                    BleLog.e(TAG, "handleMessage:++++DISCONNECT ");
////                    Log.i(TAG, "mDevices数量: " + mDevices.size());
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

    private boolean addBleDevice(BleDevice device) {
        if (device == null || mDevices.contains(device)) {
            BleLog.i(TAG, "addBleDevice" + "已经包含了该设备");
            return false;
        }
        mDevices.add(device);
        BleLog.i(TAG, "addBleDevice" + "添加了一个设备到设备池");
        return true;
    }

    public BleDevice getBleDevice(int index) {
        return mDevices.get(index);
    }

    /**
     * get BLE
     *
     * @param device blutoothdevice
     * @return bleDeive
     */
    public BleDevice getBleDevice(BluetoothDevice device) {
        if (device == null) {
            BleLog.w(TAG, "getBleDevice: " + "device is null");
            return null;
        }
        synchronized (mDevices) {
            if (mDevices.size() > 0) {
                for (BleDevice bleDevice : mDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        BleLog.w(TAG, "getBleDevice: " + "device is exist");
                        return bleDevice;
                    }
                }
            }
            BleDevice newDevice = new BleDevice(device);
            BleLog.w(TAG, "getBleDevice: " + "device is new");
            return newDevice;
        }
    }

    /**
     * Gets the connected device
     *
     * @return connected device
     */

    public ArrayList<BleDevice> getConnetedDevices() {
        return mConnetedDevices;
    }


}
