package cn.com.heaton.blelibrary.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;

import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.exception.BleServiceException;
import cn.com.heaton.blelibrary.ble.proxy.RequestImpl;
import cn.com.heaton.blelibrary.ble.proxy.RequestLisenter;
import cn.com.heaton.blelibrary.ble.proxy.RequestProxy;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ble.request.ScanRequest;

/**
 * This class provides various APIs for Bluetooth operation
 * Created by liulei on 2016/12/7.
 */

public class Ble<T extends BleDevice> implements BleLisenter<T>{

    /** Log tag, apps may override it. */
    private final static String TAG = "Ble";

    private static volatile Ble instance;

    private Options mOptions;

    private RequestLisenter<T> mRequest;

    /*private static final Map<String,List<Class<?>>> bleDeviceCache = new HashMap<>();*/

    private final Object mLocker = new Object();

    private BluetoothLeService mBluetoothLeService;

    /**
     * 打开蓝牙标志位
     */
    public static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;

    private final ArrayList<T> mAutoDevices = new ArrayList<>();

    /**
     * Initializes a newly created {@code BleManager} object so that it represents
     * a bluetooth management class .  Note that use of this constructor is
     * unnecessary since Can not be externally constructed.
     */
    private Ble() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        ///暂时注销代码后维护可能会重新使用代码
       /* Type superClass = getClass().getGenericSuperclass();
        Type type = ((ParameterizedType) superClass).getActualTypeArguments()[0];
        mDeviceClass = getClass(type,0);*/
    }

    /**
     *  蓝牙初始化
     * @param context 上下文对象
     * @param opts 蓝牙相关参数
     * @return 初始化是否成功
     */
    public boolean init(Context context,Options opts){
        if(opts == null){
            opts = new Options();
        }
        mOptions = opts;
        L.init(opts);
        /*设置动态代理*/
        mRequest = (RequestLisenter) RequestProxy
                .getInstance()
                .bindProxy(RequestImpl.getInstance(opts));

        boolean result = instance.startService(context);
        L.w(TAG, "bind service result is"+ result);
        return result;
    }

    /**
     * 开始扫描
     * @param callback 扫描回调
     */
    public void startScan(BleScanCallback<T> callback){
        mRequest.startScan(callback);
    }

    /*停止扫描*/
    public void stopScan(){
        mRequest.stopScan();
    }

    /**
     * 连接蓝牙
     *
     * @param device 蓝牙设备对象
     */
    public void connect(T device, BleConnCallback<T> callback) {
        synchronized (mLocker) {
            mRequest.connect(device, callback);
        }
    }

    /**
     * Reconnection equipment
     * <p>
     * TODO Later will add reconnection times
     *
     * @param device device
     * @return Whether the connection is successful
     */
//    private boolean reconnect(T device) {
//        // TODO: 2017/10/16 auth:Alex-Jerry  [2017/11/16]
//        return connect(device);
//    }

    /**
     * 断开蓝牙
     *
     * @param device 蓝牙设备对象
     */
    public void disconnect(T device) {
        mRequest.disconnect(device);
//        synchronized (mLocker) {
//            if (mBluetoothLeService != null) {
//                //Traverse the connected device collection to disconnect automatically cancel the automatic connection
//                for (T bleDevice : mConnetedDevices) {
//                    if (bleDevice.getBleAddress().equals(device.getBleAddress())) {
//                        Log.e(TAG, "disconnect: " + "设置自动连接false");
//                        bleDevice.setAutoConnect(false);
//                    }
//                }
//                mBluetoothLeService.disconnect(device.getBleAddress());
//            RequestManager.executeDisConnectRequest(device);
//        }
    }

    /**
     * 连接成功后，开始设置通知
     * @param device 蓝牙设备对象
     * @param callback 通知回调
     */
    public void startNotify(T device, BleNotiftCallback<T> callback){
        mRequest.notify(device, callback);
    }

    /**
     * 读取数据
     * @param device 蓝牙设备对象
     * @param callback 读取结果回调
     */
    public boolean read(T device, BleReadCallback<T> callback){
        return mRequest.read(device, callback);
    }

    /**
     * 读取远程RSSI
     * @param device 蓝牙设备对象
     * @param callback 读取远程RSSI结果回调
     */
    public void readRssi(T device, BleReadRssiCallback<T> callback){
        mRequest.readRssi(device, callback);
    }

    /**
     * 写入数据
     * @param device 蓝牙设备对象
     * @param data 写入数据字节数组
     * @param callback 写入结果回调
     * @return 写入是否成功
     */
    public boolean write(T device, byte[]data, BleWriteCallback<T> callback){
        return mRequest.write(device, data, callback);
    }

    /*获取当前类的类型*/
    public Class<T> getClassType(){
        Type genType = this.getClass().getGenericSuperclass();
        Class<T> entityClass = (Class<T>)((ParameterizedType)genType).getActualTypeArguments()[0];
        return entityClass;
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

    public static <T extends BleDevice> Ble<T> getInstance(){
        if (instance == null) {
            synchronized (Ble.class) {
                if (instance == null) {
                    instance = new Ble();
                }
            }
        }
        return instance;
    }

    /**
     * 获取自定义蓝牙服务对象
     * @return 自定义蓝牙服务对象
     */
    public BluetoothLeService getBleService() {
        return mBluetoothLeService;
    }

    /**
     * 开始绑定服务
     *
     * @return 绑定蓝牙服务是否成功
     */
    private boolean startService(Context context) {
        Intent gattServiceIntent = new Intent(context, BluetoothLeService.class);
        boolean bll = false;
        if (context != null) {
            bll = context.bindService(gattServiceIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        }
        if (bll) {
            L.i(TAG, "service bind succseed!!!");
        } else if(mOptions.throwBleException){
            try {
                throw new BleServiceException("Bluetooth service binding failed," +
                        "Please check whether the service is registered in the manifest file!");
            } catch (BleServiceException e) {
                e.printStackTrace();
            }
        }
        return bll;
    }

    /**
     * 解绑蓝牙服务
     */
    public void unService(Context context) {
        if (context != null) {
            context.unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            if(instance != null)
                mBluetoothLeService.setBleManager(instance, mOptions);

            L.e(TAG, "Service connection successful");
            if (!mBluetoothLeService.initialize()) {
                L.e(TAG, "Unable to initialize Bluetooth");
//                for (BleLisenter bleLisenter : mBleLisenters) {
//                    bleLisenter.onInitFailed();
//                }
            }
            // Automatically connects to the device upon successful start-up
            // initialization.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    /**
     * 获取指定位置的蓝牙对象
     * @param index 指定位置
     * @return 指定位置蓝牙对象
     */
    public T getBleDevice(int index) {
        ConnectRequest request = Rproxy.getInstance().getRequest(ConnectRequest.class);
        if(request != null){
            return (T) request.getBleDevice(index);
        }
        return null;
    }

    /**
     * 获取对应蓝牙对象
     * @param device 原生蓝牙对象
     * @return 对应蓝牙对象
     */
    public T getBleDevice(BluetoothDevice device) {
        ConnectRequest request = Rproxy.getInstance().getRequest(ConnectRequest.class);
        if(request != null){
            return (T) request.getBleDevice(device);
        }
        return null;
    }

    /**
     *   Get the device type   for example: BleDevice.class
     * @return device type
     */
    //Temporarily comment out the code post-maintenance may re-use the code
//    public Class<T> getDeviceClass(){
//        return mDeviceClass;
//    }

    /**
     * 获取对应锁对象
     */
    public Object getLocker() {
        return mLocker;
    }

    /**
     * 是否正在扫描
     */
    public boolean isScanning() {
        ScanRequest request = Rproxy.getInstance().getRequest(ScanRequest.class);
        return request.isScanning();
    }

    /**
     *
     * @return 已经连接的设备集合
     */

    public ArrayList<T> getConnetedDevices() {
        ConnectRequest request = Rproxy.getInstance().getRequest(ConnectRequest.class);
        if(request != null){
            return request.getConnetedDevices();
        }
        return null;
    }

//    private class AutoConThread extends Thread {
//        @Override
//        public void run() {
//            while (true) {
//                if (mAutoDevices.size() > 0) {
//                    //Turn on cyclic scan
//                    if (!mScanning) {
//                        Log.e(TAG, "run: " + "Thread began scanning...");
////                        scanLeDevice(true);
//                    }
//                }
//                SystemClock.sleep(2 * 1000);
//            }
//        }
//
//    }

    /**
     * If it is automatically connected device is removed from the automatic connection pool
     *
     * @param device Device object
     */
    private void removeAutoPool(BleDevice device) {
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
    private void addAutoPool(T device) {
        if (device == null) return;
        for (BleDevice item : mAutoDevices) {
            if (device.getBleAddress().equals(item.getBleAddress())) {
                return;
            }
        }
        if (device.isAutoConnect()) {
            L.w(TAG, "addAutoPool: "+"Add automatic connection device to the connection pool");
            mAutoDevices.add(device);
        }
    }


    /**
     * 当Application退出时，释放所有资源
     * @param context 上下文对象
     */
    public void destory(Context context){
        unService(context);
    }

    /**
     * Release Empty all resources
     */
//    public void clear() {
//        synchronized (mLocker) {
//            for (BleDevice bleDevice : mConnetedDevices) {
//                disconnect(bleDevice);
//            }
//            mConnetedDevices.clear();
//            mConnectingDevices.clear();
//        }
//    }

    /**
     *
     * @return 是否支持蓝牙
     */
    public boolean isSupportBle(Context context) {
        return (mBluetoothAdapter != null && context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    /**
     *
     * @return 蓝牙是否打开
     */
    public boolean isBleEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * 打开蓝牙
     *
     * @param activity 上下文对象
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
     * 关闭蓝牙
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * 蓝牙相关参数配置类
     */
    public static class Options extends BluetoothLeService.Options{
        /**
         * 是否打印蓝牙日志
         */
        public boolean logBleExceptions = true;
        /**
         * 是否抛出蓝牙异常
         */
        public boolean throwBleException = true;
        /**
         * 是否在蓝牙异常断开时自动连接
         */
        public boolean autoConnect = false;
        /**
         * 蓝牙连接超时时长
         */
        public int connectTimeout = 10 * 1000;
        /**
         * 蓝牙扫描周期时长
         */
        public int scanPeriod = 10 * 1000;
        /**
         * 服务绑定失败重试次数
         */
        public int serviceBindFailedRetryCount = 3;
        /**
         * 蓝牙连接失败重试次数
         */
        public int connectFailedRetryCount = 3;

    }


}
