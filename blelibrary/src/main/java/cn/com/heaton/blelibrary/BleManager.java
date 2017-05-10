package cn.com.heaton.blelibrary;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import cn.com.heaton.blelibrary.BleVO.BleDevice;

/**
 * This class provides various APIs for Bluetooth operation
 * Created by liulei on 2016/12/7.
 */

public class BleManager<T extends BleDevice> {

    private final static String TAG = "BleManager";
    public static final int REQUEST_ENABLE_BT = 1;
    private Context mContext;
    private BluetoothLeService mBluetoothLeService;
    private static BleLisenter mBleLisenter;
    public boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<T> mScanDevices = new ArrayList<>();
    private final ArrayList<T> mConnetedDevices = new ArrayList<>();
    private ArrayList<T> mConnectingDevices = new ArrayList<>();
    private final Object mLocker = new Object();
    private static BleManager instance;
    private BluetoothManager mBluetoothManager;//蓝牙管理服务
    private BleFactory<T> mBleFactory;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleConfig.ConnectTimeOut:
                    mBleLisenter.onConnectTimeOut();
                    break;
                case BleConfig.ConnectionChanged:
                    T device = null;
                    try {
                        device = mBleFactory.create(BleManager.this,(BluetoothDevice) msg.obj);
                        if (msg.arg1 == 1) {
                            //connect
                            device.setConnectionState(BleConfig.CONNECTED);
                            mConnetedDevices.add(device);
                            Log.e("ConnectionChanged","添加了一个设备");
                        } else if (msg.arg1 == 0) {
                            //disconnect
                            device.setConnectionState(BleConfig.DISCONNECT);
                            mConnetedDevices.remove(device);
                            Log.e("ConnectionChanged","移除了一个设备");
                        }else if(msg.arg1 == 2){
                            //connectting
                            device.setConnectionState(BleConfig.CONNECTING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    mBleLisenter.onConnectionChanged(device);
                    break;
                case BleConfig.Changed:
                    mBleLisenter.onChanged((BluetoothGattCharacteristic) msg.obj);
                    break;
                case BleConfig.Read:
                    mBleLisenter.onRead((BluetoothDevice) msg.obj);
                    break;
                case BleConfig.DescriptorWriter:
                    mBleLisenter.onDescriptorWriter((BluetoothGatt) msg.obj);
                    break;
                case BleConfig.ServicesDiscovered:
                    mBleLisenter.onServicesDiscovered((BluetoothGatt) msg.obj);
                    synchronized (mLocker){

                    }
                    break;
                case BleConfig.DescriptorRead:
                    mBleLisenter.onDescriptorRead((BluetoothGatt) msg.obj);
                    break;
            }
        }
    };

    public BleManager(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public static <T extends BleDevice>BleManager<T> getInstance(Context context, BleLisenter bleLisenter) throws Exception {
        mBleLisenter = bleLisenter;
        if (instance == null) {
            synchronized (BleManager.class) {
                if (instance == null) {
                    instance = new BleManager(context);
                    instance.mBleFactory = new BleFactory(context);
                }
            }
        }
        if (instance.isSupportBle()) {
            return instance;
        } else {
            throw new Exception("BLE is not supported");
        }
    }

    /**
     * Whether to support Bluetooth
     *
     * @return Whether to support Ble
     */
    public boolean isSupportBle() {
        return (mBluetoothAdapter != null && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    public BluetoothLeService getBleService() {
        return mBluetoothLeService;
    }

    /**
     * Bluetooth is turned on
     *
     * @return true  Bluetooth is turned on
     */
    public boolean isBleEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * open ble
     *
     * @param activity The context object
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
     * close ble
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * start bind service
     *
     * @return Whether the service is successfully bound
     */
    public boolean startService() {
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        boolean bll = false;
        if (mContext != null) {
            bll = mContext.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        if (bll) {
            System.out.println("---------------");
        } else {
            System.out.println("===============");
        }
        return bll;
    }

    /**
     * unbind service
     */
    public void unService() {
        if (mContext != null) {
            mContext.unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            mBluetoothLeService.setHandler(mHandler);
            Log.e(TAG, "Service connection successful");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                if (mBleLisenter != null) {
                    mBleLisenter.onInitFailed();
                }
            }
            if (mBluetoothLeService != null) mHandler.sendEmptyMessage(1);
            // Automatically connects to the device upon successful start-up
            // initialization.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     * Starts scanning or stops scanning the device
     *
     * @param enable Whether to start
     */
    public void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    mBluetoothAdapter.stopLeScan(mLeScanCallback);
                    mBleLisenter.onStop();
                }
            }, BleConfig.SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            mBleLisenter.onStart();
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mBleLisenter.onStop();
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            if(!contains(device)){
                T bleDevice = (T) new BleDevice(device);
                mBleLisenter.onLeScan(bleDevice, rssi, scanRecord);
                mScanDevices.add(bleDevice);
            }
        }
    };

    /**
     * Get the scanned bleDevice
     *
     * @return scanned bleDevice
     */
    public List<T> getScanBleDevice() {
        return mScanDevices;
    }

    public int getScanBleSize(){
        return mScanDevices.size();
    }


    public T getBleDevice(int index){
        return mScanDevices.get(index);
    }

    /**
     * Add Scanned BleDevice
     * @param device BleDevice
     */
    public void addBleDevice(T device){
        if(device == null){
            return;
        }
        synchronized (mScanDevices){
            if(mScanDevices.contains(device)){
                return;
            }
            mScanDevices.add(device);
        }
    }

    public boolean contains(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        synchronized (mScanDevices){
            for (T bleDevice : mScanDevices) {
                if (bleDevice.getBleAddress().equals(device.getAddress())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * 获取锁
     */
    public Object getLocker() {
        return mLocker;
    }

    /**
     * get bleDeive
     *
     * @param device blutoothdevice
     * @return bleDeive
     */
    public T getBleDevice(BluetoothDevice device) {
        if (device == null) {
            return null;
        }
        synchronized (mConnetedDevices){
            if(mConnetedDevices.size() > 0){
                for (T bleDevice : mConnetedDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        return bleDevice;
                    } else {
                        T newDevice = (T) new BleDevice(device);
                        return newDevice;
                    }
                }
            }
            T newDevice = (T) new BleDevice(device);
            return newDevice;
        }
    }

    /**
     * Gets the connected device
     *
     * @return connected device
     */

    public ArrayList<T> getConnetedDevices(){
        return mConnetedDevices;
    }
//    public List<BluetoothDevice> getConnectedDevices() {
//        if (mBluetoothLeService != null) {
//            return mBluetoothLeService.getConnectedDevices();
//        }
//        return null;
//    }


    /**
     * 添加正在连接的设备
     */
    public boolean addConnectingDevice(T device){
        if(device == null || mScanDevices.contains(device)){
            return false;
        }
        synchronized (mLocker){
            if(!mConnectingDevices.contains(device)){
                return false;
            }
            mConnectingDevices.add(device);
            return true;
        }
    }

    /**
     * 获取正在连接的设备
     */
    public List<T> getConnectingDevices() {
        return mConnectingDevices;
    }

    /**
     * connecte bleDevice
     *
     * @param address ble address
     */
    public boolean connect(String address) {
        synchronized (mLocker){
            boolean result = false;
            if (mBluetoothLeService != null) {
                result = mBluetoothLeService.connect(address);
            }
            return result;
        }
    }

    /**
     * disconnect device
     *
     * @param address ble address
     */
    public void disconnect(String address) {
        synchronized (mLocker){
            if (mBluetoothLeService != null) {
                mBluetoothLeService.disconnect(address);
            }
        }
    }

    /**
     * Set up notifications
     *
     * @param address        ble address
     * @param characteristic Bluetooth device object
     * @param enabled        Whether to set notifications
     */
    public void setCharacteristicNotification(String address, BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothLeService != null) {
            mBluetoothLeService.setCharacteristicNotification(address, characteristic, enabled);
        }
    }

    /**
     * send data
     *
     * @param address        ble address
     * @param data           data
     * @return Whether send success
     */
    public boolean sendData(String address, byte[] data) {
        boolean result = false;
        synchronized (mLocker){
            if (mBluetoothLeService != null) {
                result = mBluetoothLeService.wirteCharacteristic(address, data);
            }
            return result;
        }
    }

    /**
     * 获取系统蓝牙管理器
     *
     * @return 管理器对象
     */
    public BluetoothManager getBluetoothManager() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return mBluetoothManager;
    }

    //释放清空所有资源
    public void clear(){
        synchronized (mLocker){
            for (T bleDevice : mConnetedDevices){
                disconnect(bleDevice.getBleAddress());
            }
            mConnetedDevices.clear();
            mConnectingDevices.clear();
            mScanDevices.clear();
        }
    }

    /**
     * 获取程序对象
     *
     * @return 程序对象
     */
    public Context getContext() {
        return mContext;
    }


}
