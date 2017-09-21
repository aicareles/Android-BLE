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
import android.util.Log;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.List;

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
    private final ArrayList<T> mConnetedDevices = new ArrayList<>();
    private ArrayList<T> mConnectingDevices = new ArrayList<>();
    private final Object mLocker = new Object();
    private static BleManager instance;
    private BluetoothManager mBluetoothManager;//蓝牙管理服务
    private BleFactory<T> mBleFactory;
//    private final Class<T> mDeviceClass;

    private Handler mHandler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BleConfig.BleStatus.ConnectTimeOut:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onConnectTimeOut();
                    }
                    break;
                case BleConfig.BleStatus.ConnectionChanged:
                    T device = null;
                    try {
                        device = mBleFactory.create(BleManager.this,(BluetoothDevice) msg.obj);
                        if (msg.arg1 == 1) {
                            //connect
                            device.setConnectionState(BleConfig.BleStatus.CONNECTED);
                            mConnetedDevices.add(device);
//                            Log.e("ConnectionChanged","Added a device");
                        } else if (msg.arg1 == 0) {
                            //disconnect
                            device.setConnectionState(BleConfig.BleStatus.DISCONNECT);
                            mConnetedDevices.remove(device);
//                            Log.e("ConnectionChanged","Removed a device");
                        }else if(msg.arg1 == 2){
                            //connectting
                            device.setConnectionState(BleConfig.BleStatus.CONNECTING);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onConnectionChanged(device);
                    }
                    break;
                case BleConfig.BleStatus.Changed:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onChanged((BluetoothGattCharacteristic) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.Read:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onRead((BluetoothDevice) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.DescriptorWriter:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onDescriptorWriter((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.OnReady:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onReady((BluetoothDevice) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.ServicesDiscovered:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onServicesDiscovered((BluetoothGatt) msg.obj);
                    }
                    break;
                case BleConfig.BleStatus.DescriptorRead:
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onDescriptorRead((BluetoothGatt) msg.obj);
                    }
                    break;
            }
        }
    };

    protected BleManager(Context context) {
        mContext = context;
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mBleFactory = new BleFactory<>(context);
//        Type superClass = getClass().getGenericSuperclass();
//        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
//        mDeviceClass = getClass(type,0);
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

    public static <T extends BleDevice>BleManager<T> getInstance(Context context) throws Exception {
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
            throw new Exception("BLE is not supported");
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
            if(mHandler != null){
                mBluetoothLeService.setHandler(mHandler);
            }
            Log.e(TAG, "Service connection successful");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
//                if (mBleLisenter != null) {
//                    mBleLisenter.onInitFailed();
//                }
                for (BleLisenter bleLisenter : mBleLisenters){
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
                    for (BleLisenter bleLisenter : mBleLisenters){
                        bleLisenter.onStop();
                    }
                }
            }, BleConfig.SCAN_PERIOD);

            mScanning = true;
            mBluetoothAdapter.startLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters){
                bleLisenter.onStart();
            }
        } else {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            for (BleLisenter bleLisenter : mBleLisenters){
                bleLisenter.onStop();
            }
        }
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            if(!contains(device)){
                T bleDevice = (T) new BleDevice(device);
                for (BleLisenter bleLisenter : mBleLisenters){
                    bleLisenter.onLeScan(bleDevice, rssi, scanRecord);
                }
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
//    public Class<T> getDeviceClass(){
//        return mDeviceClass;
//    }
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
     * Get the lock
     */
    public Object getLocker() {
        return mLocker;
    }

    /**
     * Whether it is scanning
     */
    public boolean isScanning(){
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
            return null;
        }
        synchronized (mConnetedDevices){
            if(mConnetedDevices.size() > 0){
                for (T bleDevice : mConnetedDevices) {
                    if (bleDevice.getBleAddress().equals(device.getAddress())) {
                        return bleDevice;
                    }
                }
            }
            T newDevice = (T) new BleDevice(device);
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
     * Add the device being connected
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
     * Get the device being connected
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

    //Release Empty all resources
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
     * Get the Context object
     *
     * @return Context
     */
    public Context getContext() {
        return mContext;
    }


}
