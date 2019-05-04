package cn.com.heaton.blelibrary.ble;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.support.annotation.IntRange;
import android.support.annotation.RequiresApi;
import android.util.Log;

import java.lang.reflect.GenericArrayType;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.exception.BleServiceException;
import cn.com.heaton.blelibrary.ble.proxy.RequestImpl;
import cn.com.heaton.blelibrary.ble.proxy.RequestLisenter;
import cn.com.heaton.blelibrary.ble.proxy.RequestProxy;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ble.request.ScanRequest;

/**
 * 这个类对外提供所有的蓝牙操作API
 * Created by jerry on 2016/12/7.
 */
public class Ble<T extends BleDevice> {

    /** Log tag, apps may override it. */
    private final static String TAG = "Ble";

    private static volatile Ble sInstance;

    private static volatile Options sOptions;

    private RequestLisenter<T> mRequest;

    private final Object mLocker = new Object();

    private BluetoothLeService mBluetoothLeService;

    /**打开蓝牙标志位*/
    public static final int REQUEST_ENABLE_BT = 1;

    private BluetoothAdapter mBluetoothAdapter;

    /**
     * Initializes a newly created {@code Ble} object so that it represents
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
     * @return 初始化是否成功
     */
    public boolean init(Context context, Options options){
        sOptions = (options == null ? options() : options);
        L.init(sOptions);
        //设置动态代理
        mRequest = (RequestLisenter) RequestProxy.getInstance()
                .bindProxy(context, RequestImpl.getInstance(sOptions));
        boolean result = sInstance.startService(context);
        L.w(TAG, "bind service result is"+ result);
        return result;
    }

    public static Ble<BleDevice> create(Context context){
        Ble<BleDevice> ble = getInstance();
        ble.init(context, options());
        return ble;
    }

    /**
     * 开始扫描
     * @param callback 扫描回调
     */
    public void startScan(BleScanCallback<T> callback){
        mRequest.startScan(callback);
    }

    /**
     * 停止扫描
     */
    public void stopScan(){
        mRequest.stopScan();
    }

    /**
     * 连接蓝牙
     *
     * @param device 蓝牙设备对象
     */
    public void connect(T device, BleConnectCallback<T> callback) {
        synchronized (mLocker) {
            mRequest.connect(device, callback);
        }
    }

    /**
     * 通过mac地址连接设备
     *
     * @param address  mac地址
     * @param callback 连接回调
     */
    public void connect(String address,BleConnectCallback<T> callback){
        synchronized (mLocker) {
            mRequest.connect(address, callback);
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
    public void reconnect(T device) {
        connect(device, null);
    }

    /**
     * 断开蓝牙  无回调
     *
     * @param device 蓝牙设备对象
     */
    public void disconnect(T device) {
        mRequest.disconnect(device);
        synchronized (mLocker) {
            if (mBluetoothLeService != null) {
                //Traverse the connected device collection to disconnect automatically cancel the automatic connection
                for (T bleDevice : getConnetedDevices()) {
                    if (bleDevice.getBleAddress().equals(device.getBleAddress())) {
                        L.e(TAG, "disconnect: " + "设置自动连接false");
                        bleDevice.setAutoConnect(false);
                    }
                }
//                mBluetoothLeService.disconnect(device.getBleAddress());
//                RequestManager.executeDisConnectRequest(device);
            }
        }
    }

    /**
     * 断开蓝牙  有回调
     *
     * @param device 蓝牙设备对象
     */
    public void disconnect(T device, BleConnectCallback<T> callback) {
        mRequest.disconnect(device, callback);
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
     * 移除通知
     * @param  device 蓝牙设备对象
     */
    public void cancelNotify(T device){
        mRequest.unNotify(device);
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
     * 设置MTU
     * @param address 蓝牙设备地址
     * @param mtu mtu大小
     * @return 是否设置成功
     */
    public boolean setMTU(String address, int mtu, BleMtuCallback<T> callback){
        return mRequest.setMtu(address, mtu, callback);
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

    /**
     * 写入大数据量的数据（分包）
     * @param device 蓝牙设备对象
     * @param data 写入的总字节数组（如整个文件的字节数组）
     * @param packLength 每包需要发送的长度
     * @param delay 每包之间的时间间隔
     * @param callback 发送结果回调
     */
    public void writeEntity(T device, final byte[]data, @IntRange(from = 1,to = 20)int packLength, int delay, BleWriteEntityCallback<T> callback){
        mRequest.writeEntity(device, data, packLength, delay, callback);
    }

    public void cancelWriteEntity(){
        mRequest.cancelWriteEntity();
    }

    /**
     * 开始发送广播包
     * @param payload 负载数据
     */
    public void startAdvertising(byte[] payload) {
        mRequest.startAdvertising(payload);
    }

    /**
     * 停止发送广播包
     */
    public void stopAdvertising() {
        mRequest.stopAdvertising();
    }

//    public boolean writeAutoEntity(T device, final byte[]data, int packLength){
//        return mRequest.writeAutoEntity(device, data, packLength);
//    }

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
        if (sInstance == null) {
            synchronized (Ble.class) {
                if (sInstance == null) {
                    sInstance = new Ble();
                }
            }
        }
        return sInstance;
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
        } else if(sOptions.throwBleException){
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
        if (context != null && mBluetoothLeService != null) {
            context.unbindService(mServiceConnection);
            mBluetoothLeService = null;
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();

            if(sInstance != null)
                mBluetoothLeService.initialize(sOptions);

            L.e(TAG, "Service connection successful");
            if (!mBluetoothLeService.initBLE()) {
                L.e(TAG, "Unable to initBLE Bluetooth");
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
     * 根据蓝牙地址获取蓝牙对象
     * @param address 蓝牙地址
     * @return 对应的蓝牙对象
     */
    public T getBleDevice(String address){
        ConnectRequest request = Rproxy.getInstance().getRequest(ConnectRequest.class);
        if(request != null){
            return (T) request.getBleDevice(address);
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
   /*  public Class<T> getDeviceClass(){
        return mDeviceClass;
    }*/

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
        ConnectRequest<T> request = Rproxy.getInstance().getRequest(ConnectRequest.class);
        if(request != null){
            return request.getConnetedDevices();
        }
        return null;
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
    /*public void clear() {
        synchronized (mLocker) {
            for (BleDevice bleDevice : mConnetedDevices) {
                disconnect(bleDevice);
            }
            mConnetedDevices.clear();
            mConnectingDevices.clear();
        }
    }*/

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
     * 打开蓝牙(默认模式--带系统弹出框)
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
     * 强制打开蓝牙（不弹出系统弹出框）
     */
    public void turnOnBlueToothNo(){
        if(!isBleEnable()){
            mBluetoothAdapter.enable();
        }
    }

    /**
     * 关闭蓝牙
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * 清理蓝牙缓存
     * @param address 蓝牙设备地址
     * @return 是否清理成功
     */
    public boolean refreshDeviceCache(String address) {
        if (mBluetoothLeService != null) {
            return mBluetoothLeService.refreshDeviceCache(address);
        }
        return false;
    }

    /**
     * 设置MTU
     * @param address 蓝牙设备地址
     * @param mtu mtu大小
     * @return 是否设置成功
     */
    public boolean setMTU(String address, int mtu){
        if (mBluetoothLeService != null){
            return mBluetoothLeService.setMTU(address, mtu);
        }
        return false;
    }

    public static Options options(){
        if(sOptions == null){
            sOptions = new Options();
        }
        return sOptions;
    }


    /**
     * 蓝牙相关参数配置类
     */
    public static class Options {
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
        public long connectTimeout = 10 * 1000L;
        /**
         * 蓝牙扫描周期时长
         */
        public long scanPeriod = 10 * 1000L;
        /**
         * 服务绑定失败重试次数
         */
        public int serviceBindFailedRetryCount = 3;
        /**
         * 蓝牙连接失败重试次数
         */
        public int connectFailedRetryCount = 3;

        /**
         * 广播包,厂商id
         */
        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public int manufacturerId = 65520; // 0xfff0

        public Options setScanPeriod(long scanPeriod){
            this.scanPeriod = scanPeriod;
            return this;
        }

        public boolean isLogBleExceptions() {
            return logBleExceptions;
        }

        public Options setLogBleExceptions(boolean logBleExceptions) {
            this.logBleExceptions = logBleExceptions;
            return this;
        }

        public boolean isThrowBleException() {
            return throwBleException;
        }

        public Options setThrowBleException(boolean throwBleException) {
            this.throwBleException = throwBleException;
            return this;
        }

        public boolean isAutoConnect() {
            return autoConnect;
        }

        public Options setAutoConnect(boolean autoConnect) {
            this.autoConnect = autoConnect;
            return this;
        }

        public long getConnectTimeout() {
            return connectTimeout;
        }

        public Options setConnectTimeout(long connectTimeout) {
            this.connectTimeout = connectTimeout;
            return this;
        }

        public long getScanPeriod() {
            return scanPeriod;
        }

        public int getServiceBindFailedRetryCount() {
            return serviceBindFailedRetryCount;
        }

        public Options setServiceBindFailedRetryCount(int serviceBindFailedRetryCount) {
            this.serviceBindFailedRetryCount = serviceBindFailedRetryCount;
            return this;
        }

        public int getConnectFailedRetryCount() {
            return connectFailedRetryCount;
        }

        public Options setConnectFailedRetryCount(int connectFailedRetryCount) {
            this.connectFailedRetryCount = connectFailedRetryCount;
            return this;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public int getManufacturerId() {
            return manufacturerId;
        }

        @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
        public void setManufacturerId(int manufacturerId) {
            this.manufacturerId = manufacturerId;
        }

        UUID[] uuid_services_extra = new UUID[]{};
        UUID uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");
        UUID uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
        UUID uuid_read_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
        UUID uuid_notify = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");
        UUID uuid_notify_desc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

        UUID uuid_ota_service = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb");
        UUID uuid_ota_notify_cha = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16");
        UUID uuid_ota_write_cha = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16");

        public UUID[] getUuid_services_extra() {
            return uuid_services_extra;
        }

        public Options setUuid_services_extra(UUID[] uuid_services_extra) {
            this.uuid_services_extra = uuid_services_extra;
            return this;
        }

        public UUID getUuid_service() {
            return uuid_service;
        }

        public Options setUuid_service(UUID uuid_service) {
            this.uuid_service = uuid_service;
            return this;
        }

        public UUID getUuid_write_cha() {
            return uuid_write_cha;
        }

        public Options setUuid_write_cha(UUID uuid_write_cha) {
            this.uuid_write_cha = uuid_write_cha;
            return this;
        }

        public UUID getUuid_read_cha() {
            return uuid_read_cha;
        }

        public Options setUuid_read_cha(UUID uuid_read_cha) {
            this.uuid_read_cha = uuid_read_cha;
            return this;
        }

        public UUID getUuid_notify() {
            return uuid_notify;
        }

        public Options setUuid_notify(UUID uuid_notify) {
            this.uuid_notify = uuid_notify;
            return this;
        }

        public UUID getUuid_notify_desc() {
            return uuid_notify_desc;
        }

        public Options setUuid_notify_desc(UUID uuid_notify_desc) {
            this.uuid_notify_desc = uuid_notify_desc;
            return this;
        }

        public UUID getUuid_ota_service() {
            return uuid_ota_service;
        }

        public Options setUuid_ota_service(UUID uuid_ota_service) {
            this.uuid_ota_service = uuid_ota_service;
            return this;
        }

        public UUID getUuid_ota_notify_cha() {
            return uuid_ota_notify_cha;
        }

        public Options setUuid_ota_notify_cha(UUID uuid_ota_notify_cha) {
            this.uuid_ota_notify_cha = uuid_ota_notify_cha;
            return this;
        }

        public UUID getUuid_ota_write_cha() {
            return uuid_ota_write_cha;
        }

        public Options setUuid_ota_write_cha(UUID uuid_ota_write_cha) {
            this.uuid_ota_write_cha = uuid_ota_write_cha;
            return this;
        }

        public Ble<BleDevice> create(Context context){
            return Ble.create(context);
        }

    }


}
