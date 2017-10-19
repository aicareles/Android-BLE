package cn.com.heaton.blelibrary.ble;

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
import android.os.SystemClock;
import android.util.Log;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

import cn.com.heaton.blelibrary.ble.exception.BleNotSupportException;
import cn.com.heaton.blelibrary.ble.exception.BleServiceException;

/**
 * This class provides various APIs for Bluetooth operation
 * Created by liulei on 2016/12/7.
 */

public class BleManager<T extends BleDevice> {

    private final static String TAG = "BleManager";
    public static final int REQUEST_ENABLE_BT = 1;
    private Context mContext;
    private BluetoothLeService mBluetoothLeService;
    //    private static BleLisenter mBleLisenter;
    private static List<BleLisenter> mBleLisenters = new ArrayList<>();
    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private final ArrayList<T> mScanDevices = new ArrayList<>();
    private final ArrayList<T> mDevices = new ArrayList<>();
    private final ArrayList<T> mConnetedDevices = new ArrayList<>();
    private final ArrayList<T> mAutoDevices = new ArrayList<>();
    private ArrayList<T> mConnectingDevices = new ArrayList<>();
    private final Object mLocker = new Object();
    private static BleManager instance;
    private BluetoothManager mBluetoothManager;//蓝牙管理服务
    private BleFactory<T> mBleFactory;

    //    private final Class<T> mDeviceClass;
    private class AutoConThread extends Thread {
        @Override
        public void run() {
            while (BleConfig.isAutoConnect) {
                if (mAutoDevices.size() > 0) {
                    //开启循环扫描
                    if (!mScanning) {
                        Log.e(TAG, "run: " + "线程开始扫描++++");
                        scanLeDevice(true);
                    }
                }
                SystemClock.sleep(2 * 1000);
            }
        }

    }


    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleStates.BleStatus.ConnectException:
                    T t = getBleDevice((BluetoothDevice) msg.obj);
                    int errorCode = msg.arg1;
                    //断开并清除连接
                    if (null != t) {
                        for (BleLisenter bleLisenter : mBleLisenters) {
                            bleLisenter.onConnectException(t,errorCode);
                        }
                        mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 0, 0, msg.obj).sendToTarget();
                    }
                    break;
                case BleStates.BleStatus.ConnectionChanged:
                    T device = null;
                    try {
                        device = mBleFactory.create(BleManager.this, (BluetoothDevice) msg.obj);
                        if (msg.arg1 == 1) {
                            //connect
                            device.setConnectionState(BleStates.BleStatus.CONNECTED);
                            mConnetedDevices.add(device);
                            //连接成功后 才能被认为可以自动重连
                            device.setAutoConnect(true);
                            //如果是自动连接的设备  则从自动连接池中移除
                            removeAutoPool(device);
                        } else if (msg.arg1 == 0) {
                            //disconnect
                            device.setConnectionState(BleStates.BleStatus.DISCONNECT);
                            mConnetedDevices.remove(device);
                            mDevices.remove(device);
                            Log.i(TAG, "mDevices数量: "+mDevices.size());
                            addAutoPool(device);
                        } else if (msg.arg1 == 2) {
                            //connectting
                            device.setConnectionState(BleStates.BleStatus.CONNECTING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onConnectionChanged(device);
                    }
                    break;
                case BleStates.BleStatus.Changed:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onChanged((BluetoothGattCharacteristic) msg.obj);
                    }
                    break;
                case BleStates.BleStatus.Read:
                    T r = getBleDevice((BluetoothDevice) msg.obj);
                    if (r != null) {
                        for (BleLisenter bleLisenter : mBleLisenters) {
                            bleLisenter.onRead(r);
                        }
                    }
                    break;
                case BleStates.BleStatus.DescriptorWriter:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onDescriptorWriter((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleStates.BleStatus.OnReady:
                    T o = getBleDevice((BluetoothDevice) msg.obj);
                    if (o != null) {
                        for (BleLisenter bleLisenter : mBleLisenters) {
                            bleLisenter.onReady(o);
                        }
                    }
                    break;
                case BleStates.BleStatus.ServicesDiscovered:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onServicesDiscovered((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleStates.BleStatus.DescriptorRead:
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onDescriptorRead((BluetoothGatt) msg.obj);
                    }
                    break;
            }
        }
    };

    /**
     * 如果是自动连接的设备则从自动连接池中移除
     *
     * @param device 设备对象
     */
    private void removeAutoPool(T device) {
        if (device == null) return;
        Iterator<T> iterator = mAutoDevices.iterator();
        while (iterator.hasNext()) {
            T item = iterator.next();
            if (device.getBleAddress().equals(item.getBleAddress())) {
                iterator.remove();
            }
        }
//        for (int i = 0; i < mAutoDevices.size(); i++) {
//            if (device.getBleAddress().equals(mAutoDevices.get(i).getBleAddress())) {
//                Log.e(TAG, "removeAuto: " + "移除自动连接的设备");
//                mAutoDevices.remove(i);
//            }
//        }
    }

    /**
     * 将断开设备添加到自动连接池中
     *
     * @param device 设备对象
     */
    private void addAutoPool(T device) {
        if (device == null) return;
        for (T item : mAutoDevices) {
            if (device.getBleAddress().equals(item.getBleAddress())) {
                return;
            }
        }
        if (device.isAutoConnect()) {
            Log.w(TAG, "addAutoPool: "+"添加自动连接设备到连接池");
            mAutoDevices.add(device);
        }
    }

    /**
     * Initializes a newly created {@code BleManager} object so that it represents
     * a bluetooth management class .  Note that use of this constructor is
     * unnecessary since Can not be externally constructed.
     */
    private BleManager(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleFactory = new BleFactory<>(context);
        new AutoConThread().start();
        ///暂时注释掉该段代码   后期维护可能会重新使用该段代码
       /* Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        mDeviceClass = getClass(type,0);*/
    }

    /**
     * Get the class object
     *
     * @param type TYPE
     * @param i    LOCATION
     * @return Object
     */
    private static Class getClass(Type type, int i) {
        if (type instanceof ParameterizedType) { //Processing generic types
            return getGenericClass((ParameterizedType) type, i);
        } else if (type instanceof TypeVariable) {
            return getClass(((TypeVariable) type).getBounds()[0], 0); // Handle the generic wipe object
        } else {// Class itself is also type, forced transformation
            return (Class) type;
        }
    }

    private static Class getGenericClass(ParameterizedType parameterizedType, int i) {
        Object genericClass = parameterizedType.getActualTypeArguments()[i];
        if (genericClass instanceof ParameterizedType) { // Processing multistage generic
            return (Class) ((ParameterizedType) genericClass).getRawType();
        } else if (genericClass instanceof GenericArrayType) { // Processing array generics
            return (Class) ((GenericArrayType) genericClass).getGenericComponentType();
        } else if (genericClass instanceof TypeVariable) { //Handle the generic wipe object
            return getClass(((TypeVariable) genericClass).getBounds()[0], 0);
        } else {
            return (Class) genericClass;
        }
    }

    public static <T extends BleDevice> BleManager<T> getInstance(Context context) throws BleNotSupportException {
//        mBleLisenter = bleLisenter;
        if (instance == null) {
            synchronized (BleManager.class) {
                if (instance == null) {
                    instance = new BleManager(context);
                }
            }
        }
        if (instance.isSupportBle()) {
            return instance;
        } else {
            throw new BleNotSupportException("BLE is not supported");
        }
    }

    /**
     * Whether to support Bluetooth
     *
     * @return Whether to support Ble
     */
    private boolean isSupportBle() {
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
            Log.i(TAG, "service bind succseed!!!");
        } else {
            try {
                throw new BleServiceException("Bluetooth service binding failed");
            } catch (BleServiceException e) {
                startService();
                e.printStackTrace();
            }
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
            if (mHandler != null)
                mBluetoothLeService.setHandler(mHandler);
            if(instance != null)
                mBluetoothLeService.setBleManager(instance);

            Log.e(TAG, "Service connection successful");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                for (BleLisenter bleLisenter : mBleLisenters) {
                    bleLisenter.onInitFailed();
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
                    for (BleLisenter bleLisenter : mBleLisenters) {
                        bleLisenter.onStop();
                    }
                }
            }, BleConfig.SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters) {
                bleLisenter.onStart();
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters) {
                bleLisenter.onStop();
            }
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            if (device == null) return;
            if (!contains(device)) {
                T bleDevice = (T) new BleDevice(device);
                for (BleLisenter bleLisenter : mBleLisenters) {
                    bleLisenter.onLeScan(bleDevice, rssi, scanRecord);
                }
                mScanDevices.add(bleDevice);
            } else {
                synchronized (mLocker) {
                    for (T autoDevice : mAutoDevices) {
                        if (device.getAddress().equals(autoDevice.getBleAddress())) {
                            //说明非主动断开设备   理论上需要自动重新连接（前提是连接时设置自动连接属性为true）
                            if (!autoDevice.isConnected() && !autoDevice.isConnectting() && autoDevice.isAutoConnect()) {
                                Log.e(TAG, "onLeScan: " + "正在重连设备...");
                                reconnect(autoDevice);
                                mAutoDevices.remove(autoDevice);
                            }
                        }
                    }
                }
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

    public int getScanBleSize() {
        return mScanDevices.size();
    }


    public T getBleDevice(int index) {
        return mScanDevices.get(index);
    }


    /**
     * Get the listener
     *
     * @return Listener object
     */
    public List<BleLisenter> getBleListeners() {
        return mBleLisenters;
    }

    /**
     *   Get the device type   for example: BleDevice.class
     * @return
     */
    ///暂时注释掉该段代码   后期维护可能会重新使用该段代码
    /*public Class<T> getDeviceClass(){
        return mDeviceClass;
    }*/

    /**
     * Add BleDevice push device  pool
     *
     * @param device BleDevice
     */
    private void addBleDevice(T device) {
        if (device == null || mDevices.contains(device)) {
            Log.i(TAG, "addBleDevice" + "已经包含了该设备");
            return;
        }
        mDevices.add(device);
        Log.i(TAG, "addBleDevice" + "添加了一个设备到设备池");
    }

    private boolean contains(BluetoothDevice device) {
        if (device == null) {
            return false;
        }
        synchronized (mScanDevices) {
            for (T bleDevice : mScanDevices) {
                if (bleDevice.getBleAddress().equals(device.getAddress())) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * Get the lock
     */
    public Object getLocker() {
        return mLocker;
    }

    /**
     * Whether it is scanning
     */
    public boolean isScanning() {
        return mScanning;
    }

    /**
     * get BLE
     *
     * @param device blutoothdevice
     * @return bleDeive
     */
    public T getBleDevice(BluetoothDevice device) {
        if (device == null) {
            Log.w(TAG, "getBleDevice: "+"device is null");
            return null;
        }
        synchronized (mDevices) {
            if (mDevices.size() > 0) {
                for (T bleDevice : mDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        Log.w(TAG, "getBleDevice: "+"device is exist");
                        return bleDevice;
                    }
                }
            }
            T newDevice = (T) new BleDevice(device);
            Log.w(TAG, "getBleDevice: "+"device is new");
            return newDevice;
        }
    }

    /**
     * Register the listening event
     *
     * @param bleListener Listener
     */
    public void registerBleListener(BleLisenter bleListener) {
        if (mBleLisenters.contains(bleListener)) {
            return;
        }
        mBleLisenters.add(bleListener);
    }

    /**
     * Cancel the registration event
     *
     * @param bleListener Listener
     */
    public void unRegisterBleListener(BleLisenter bleListener) {
        if (bleListener == null) {
            return;
        }
        if (mBleLisenters.contains(bleListener)) {
            mBleLisenters.remove(bleListener);
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

    /**
     * Add the device being connected
     */
    public boolean addConnectingDevice(T device) {
        if (device == null || mScanDevices.contains(device)) {
            return false;
        }
        synchronized (mLocker) {
            if (!mConnectingDevices.contains(device)) {
                return false;
            }
            mConnectingDevices.add(device);
            return true;
        }
    }

    /**
     * Get the device being connected
     */
    public List<T> getConnectingDevices() {
        return mConnectingDevices;
    }

    /**
     * connecte bleDevice
     *
     * @param device
     * @return
     */
    public boolean connect(T device) {
        synchronized (mLocker) {
            addBleDevice(device);
            boolean result = false;
            if (mBluetoothLeService != null) {
                result = mBluetoothLeService.connect(device.getBleAddress());
            }
            return result;
        }
    }

    /**
     * 重连设备
     * <p>
     * TODO 后期会添加重连次数
     *
     * @param device 设备
     * @return 是否连接成功
     */
    private boolean reconnect(T device) {
        // TODO: 2017/10/16 auth:Alex-Jerry  [2017/11/16]
        return connect(device);
    }

    /**
     * disconnect device
     *
     * @param device ble address
     */
    public void disconnect(T device) {
        synchronized (mLocker) {
            if (mBluetoothLeService != null) {
                //遍历已连接设备集合  主动断开则取消自动连接
                for (T bleDevice : mConnetedDevices) {
                    if (bleDevice.getBleAddress().equals(device.getBleAddress())) {
                        Log.e(TAG, "disconnect: " + "设置自动连接false");
                        bleDevice.setAutoConnect(false);
                    }
                }
                mBluetoothLeService.disconnect(device.getBleAddress());
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
     * @param address ble address
     * @param data    data
     * @return Whether send success
     */
    public boolean sendData(String address, byte[] data) {
        boolean result = false;
        synchronized (mLocker) {
            if (mBluetoothLeService != null) {
                result = mBluetoothLeService.wirteCharacteristic(address, data);
            }
            return result;
        }
    }


    /**
     * Get the system Bluetooth manager
     *
     * @return Manager object
     */
    public BluetoothManager getBluetoothManager() {
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) mContext.getSystemService(Context.BLUETOOTH_SERVICE);
        }
        return mBluetoothManager;
    }

    /**
     * Release Empty all resources
     */
    public void clear() {
        synchronized (mLocker) {
            for (T bleDevice : mConnetedDevices) {
                disconnect(bleDevice);
            }
            mConnetedDevices.clear();
            mConnectingDevices.clear();
            mScanDevices.clear();
        }
    }

    /**
     * Get the Context object
     *
     * @return Context
     */
    public Context getContext() {
        return mContext;
    }


}
