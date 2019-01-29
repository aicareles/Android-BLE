package cn.com.heaton.blelibrary.ble;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.com.heaton.blelibrary.BuildConfig;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ConnectWrapperLisenter;
import cn.com.heaton.blelibrary.ble.callback.wrapper.NotifyWrapperLisenter;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.request.NotifyRequest;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ota.OtaListener;


@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private ConnectRequest mConnectRequest;
    private Handler mHandler;
    private Ble.Options mOptions;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final Object mLocker = new Object();
    //    private BluetoothGattCharacteristic mWriteCharacteristic;//Writable GattCharacteristic object
    private List<BluetoothGattCharacteristic> mNotifyCharacteristics = new ArrayList<>();//Notification attribute callback array
    private int mNotifyIndex = 0;//Notification feature callback list
    private int mIndex = 0;//Device index
    private BluetoothGattCharacteristic mOtaWriteCharacteristic;//Ota ble send the object
    private boolean mOtaUpdating = false;//Whether the OTA is updated

    private Map<String, BluetoothGattCharacteristic> mWriteCharacteristicMap = new HashMap<>();

    private Map<String, BluetoothGattCharacteristic> mReadCharacteristicMap = new HashMap<>();

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    /**
     * The address of the connected device
     */
    private List<String> mConnectedAddressList;

    private ConnectWrapperLisenter mConnectWrapperLisenter;

    private NotifyWrapperLisenter mNotifyWrapperLisenter;

    private OtaListener mOtaListener;//Ota update operation listener

    //blutooth status observer
    private       BluetoothChangedObserver mBleObserver;

    /**
     * 在各种状态回调中发现连接更改或服务
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            BluetoothDevice device = gatt.getDevice();
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mIndex++;
                    mConnectedAddressList.add(device.getAddress());
                    mHandler.removeMessages(BleStates.BleStatus.ConnectTimeOut);
//                    mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 1, 0, device).sendToTarget();
                    if (mConnectWrapperLisenter != null){
                        mConnectWrapperLisenter.onConnectionChanged(device, BleStates.BleStatus.CONNECTED);
                    }
                    L.i(TAG, "handleMessage:++++CONNECTED.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery");
                    mBluetoothGattMap.get(device.getAddress()).discoverServices();
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    mHandler.removeMessages(BleStates.BleStatus.ConnectTimeOut);
                    L.i(TAG, "Disconnected from GATT server.");
//                    mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 0, 0, device).sendToTarget();
                    if (mConnectWrapperLisenter != null){
                        mConnectWrapperLisenter.onConnectionChanged(device, BleStates.BleStatus.DISCONNECT);
                    }
                    close(device.getAddress());
                }
            } else {
                //Occurrence 133 or 257 19 Equal value is not 0: Connection establishment failed due to protocol stack
                mHandler.removeMessages(BleStates.BleStatus.ConnectTimeOut);
                L.e(TAG, "onConnectionStateChange+++: " + "Connection status is abnormal:" + status);
                close(device.getAddress());
//                mHandler.obtainMessage(BleStates.BleStatus.ConnectException, errorCode, 0, device).sendToTarget();
                if (mConnectWrapperLisenter != null){
                    mConnectWrapperLisenter.onConnectException(device);
                }
            }

        }

        @TargetApi(Build.VERSION_CODES.LOLLIPOP)
        public void onMtuChanged(android.bluetooth.BluetoothGatt gatt, int mtu, int status){
            if (gatt != null && gatt.getDevice() != null) {
                BleDevice d = mConnectRequest.getBleDevice(gatt.getDevice());
                L.e(TAG, "onMtuChanged mtu=" + mtu + ",status=" + status);
                mHandler.obtainMessage(BleStates.BleStatus.MTUCHANGED, mtu, status, d).sendToTarget();
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
//                mHandler.obtainMessage(BleStates.BleStatus.ServicesDiscovered, gatt).sendToTarget();
                if (mNotifyWrapperLisenter != null) {
                    mNotifyWrapperLisenter.onServicesDiscovered(gatt);
                }
                //Empty the notification attribute list
                mNotifyCharacteristics.clear();
                mNotifyIndex = 0;
                //Start setting notification feature
                displayGattServices(gatt.getDevice().getAddress(), getSupportedGattServices(gatt.getDevice().getAddress()));
            } else {
                L.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            L.d(TAG, "onCharacteristicRead:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleStates.BleStatus.Read, characteristic).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("--------write success----- status:" + status);
            synchronized (mLocker) {
                if (BuildConfig.DEBUG) {
                    L.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicWrite: " + status);
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mOptions.uuid_ota_write_cha.equals(characteristic.getUuid())) {
                        if (mOtaListener != null) {
                            mOtaListener.onWrite();
                        }
                        return;
                    }
                    mHandler.obtainMessage(BleStates.BleStatus.Write, characteristic).sendToTarget();
                }
            }
        }

        /**
         * 当连接成功的时候会回调这个方法，这个方法可以处理发送密码或者数据分析
         * 当setnotify（true）被设置时，如果MCU（设备端）上的数据改变，则该方法被回调。
         * @param gatt 蓝牙gatt对象
         * @param characteristic 蓝牙通知特征对象
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            synchronized (mLocker) {
                if (gatt.getDevice() == null)return;

                BleDevice d = mConnectRequest.getBleDevice(gatt.getDevice());
                L.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicChanged: " + (characteristic.getValue() != null ? Arrays.toString(characteristic.getValue()) : ""));
                if (mOptions.uuid_ota_write_cha.equals(characteristic.getUuid()) || mOptions.uuid_ota_notify_cha.equals(characteristic.getUuid())) {
                    if (mOtaListener != null) {
                        mOtaListener.onChange(characteristic.getValue());
                    }
                    return;
                }

                if(d != null){
                    d.setNotifyCharacteristic(characteristic);
                    if (mNotifyWrapperLisenter != null) {
                        mNotifyWrapperLisenter.onChanged(d, characteristic);
                    }
                }
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getCharacteristic().getUuid();
            L.w(TAG, "onDescriptorWrite");
            L.e(TAG, "descriptor_uuid:" + uuid);
            synchronized (mLocker) {
                L.e(TAG, " -- onDescriptorWrite: " + status);
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0 && mNotifyIndex < mNotifyCharacteristics.size()) {
                        setCharacteristicNotification(gatt.getDevice().getAddress(), mNotifyCharacteristics.get(mNotifyIndex++), true);
                    } else {
                        L.e(TAG, "====setCharacteristicNotification is true,ready to sendData===");
//                        mHandler.obtainMessage(BleStates.BleStatus.NotifySuccess, gatt).sendToTarget();
                        if (mNotifyWrapperLisenter != null) {
                            mNotifyWrapperLisenter.onNotifySuccess(gatt);
                        }
                    }
                }
                mHandler.obtainMessage(BleStates.BleStatus.DescriptorWriter, gatt).sendToTarget();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            UUID uuid = descriptor.getCharacteristic().getUuid();
            L.w(TAG, "onDescriptorRead");
            L.e(TAG, "descriptor_uuid:" + uuid);
            mHandler.obtainMessage(BleStates.BleStatus.DescriptorRead, gatt).sendToTarget();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("rssi = " + rssi);
            mHandler.obtainMessage(BleStates.BleStatus.ReadRssi, rssi).sendToTarget();
        }
    };

    /**
     * 蓝牙状态变化时的回调  2018/08/29
     */
    private BluetoothChangedObserver.BluetoothStatusLisenter
            mBluetoothStatusLisenter = new BluetoothChangedObserver.BluetoothStatusLisenter() {
        @Override
        public void onBluetoothStatusChanged(int status) {
            mHandler.obtainMessage(status).sendToTarget();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

    /**
     *
     * @return 已经连接的设备集合
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if (mBluetoothManager == null) return null;
        return mBluetoothManager.getConnectedDevices(BluetoothProfile.GATT);
    }


    public class LocalBinder extends Binder {
        public BluetoothLeService getService() {
            return BluetoothLeService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        this.mBleObserver = new BluetoothChangedObserver(getApplication());
        this.mBleObserver.setBluetoothStatusLisenter(mBluetoothStatusLisenter);
        //注册广播接收器
        this.mBleObserver.registerReceiver();
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        mBleObserver.unregisterReceiver();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public void initialize(Ble.Options options) {
        mConnectRequest = Rproxy.getInstance().getRequest(ConnectRequest.class);
        setConnectWrapperLisenter(mConnectRequest);
        NotifyRequest request = Rproxy.getInstance().getRequest(NotifyRequest.class);
        setNotifyWrapperLisenter(request);
        this.mHandler = BleHandler.getHandler();
        this.mOptions = options;
    }

    /**
     * 初始化蓝牙
     *
     * @return 是否初始化成功
     */
    public boolean initBLE() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        //Bluetooth 4.0, that API level> = 18, and supports Bluetooth 4.0 phone can use, if the mobile phone system version API level <18, is not used Bluetooth 4 android system 4.3 above, the phone supports Bluetooth 4.0
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                L.e(TAG, "Unable to initBLE BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            L.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * 连接蓝牙
     *
     * @param address 蓝牙地址
     * @return 是否连接成功
     */
    // TODO: 2017/6/6  connect
    public boolean connect(final String address) {
        if (mConnectedAddressList == null) {
            mConnectedAddressList = new ArrayList<>();
        }
        if (mConnectedAddressList.contains(address)) {
            L.d(TAG, "This is device already connected.");
            return true;
        }

        if (mBluetoothAdapter == null) {
            L.w(TAG,
                    "BluetoothAdapter not initialized");
            return false;
        }

        // getRemoteDevice(address) will throw an exception if the device address is invalid,
        // so it's necessary to check the address
        boolean isValidAddress = BluetoothAdapter.checkBluetoothAddress(address);
        if (!isValidAddress) {
            L.d(TAG, "the device address is invalid");
            return false;
        }

        // Previously connected device. Try to reconnect. ()
        if (mBluetoothGattMap == null) {
            mBluetoothGattMap = new HashMap<>();
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            L.d(TAG, "no device");
            return false;
        }
        //10s after the timeout prompt
        Message msg = Message.obtain();
        msg.what = BleStates.BleStatus.ConnectTimeOut;
        msg.obj = device;
        mHandler.sendMessageDelayed(msg, mOptions.connectTimeout);
//        mHandler.obtainMessage(BleStates.BleStatus.ConnectionChanged, 2, 0, device).sendToTarget();
        if (mConnectWrapperLisenter != null){
            mConnectWrapperLisenter.onConnectionChanged(device, BleStates.BleStatus.CONNECTING);
        }
        // We want to directly connect to the device, so we are setting the autoConnect parameter to false
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt != null) {
            mBluetoothGattMap.put(address, bluetoothGatt);
            L.d(TAG, "Trying to create a new connection.");
            return true;
        }
        return false;
    }

    /**
     * 断开蓝牙
     *
     * @param address 蓝牙地址
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mNotifyIndex = 0;
        mBluetoothGattMap.get(address).disconnect();
        mNotifyCharacteristics.clear();
        mWriteCharacteristicMap.remove(address);
        mReadCharacteristicMap.remove(address);
        mOtaWriteCharacteristic = null;
    }

    /**
     * 清除蓝牙蓝牙连接设备的指定蓝牙地址
     *
     * @param address 蓝牙地址
     */
    public void close(String address) {
        mConnectedAddressList.remove(address);
        if (mBluetoothGattMap.get(address) != null) {
            mBluetoothGattMap.get(address).close();
            mBluetoothGattMap.remove(address);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public boolean setMTU(String address, int mtu){
        L.d(TAG,"setMTU "+mtu);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            if(mtu>20){
                if (mBluetoothGattMap.get(address) != null) {
                    boolean result =   mBluetoothGattMap.get(address).requestMtu(mtu);
                    L.d(TAG,"requestMTU "+mtu+" result="+result);
                    return result;
                }
            }
        }
        return false;
    }

    /**
     * 清除所有可连接的设备
     */
    public void close() {
        if (mConnectedAddressList == null) return;
        for (String address :
                mConnectedAddressList) {
            if (mBluetoothGattMap.get(address) != null) {
                mBluetoothGattMap.get(address).close();
            }
        }
        mBluetoothGattMap.clear();
        mConnectedAddressList.clear();
    }

    /**
     * 清理蓝牙缓存
     */
    public boolean refreshDeviceCache(String address) {
        BluetoothGatt gatt = mBluetoothGattMap.get(address);
        if (gatt != null) {
            try {
                Method localMethod = gatt.getClass().getMethod(
                        "refresh", new Class[0]);
                if (localMethod != null) {
                    boolean bool = ((Boolean) localMethod.invoke(
                            gatt, new Object[0])).booleanValue();
                    return bool;
                }
            } catch (Exception localException) {
                L.i(TAG, "An exception occured while refreshing device");
            }
        }
        return false;
    }


    /**
     * 写入数据
     *
     * @param address 蓝牙地址
     * @param value   发送的字节数组
     * @return 写入是否成功(这个是客户端的主观认为)
     */
    public boolean wirteCharacteristic(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = mWriteCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            try {
                if (mOptions.uuid_write_cha.equals(gattCharacteristic.getUuid())) {
                    gattCharacteristic.setValue(value);
                    boolean result = mBluetoothGattMap.get(address).writeCharacteristic(gattCharacteristic);
                    L.d(TAG, address + " -- write data:" + Arrays.toString(value));
                    L.d(TAG, address + " -- write result:" + result);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;

    }

    /**
     * 读取数据
     *
     * @param address 蓝牙地址
     * @return 读取是否成功(这个是客户端的主观认为)
     */
    public boolean readCharacteristic(String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = mReadCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            try {
                if (mOptions.uuid_read_cha.equals(gattCharacteristic.getUuid())) {
                    boolean result = mBluetoothGattMap.get(address).readCharacteristic(gattCharacteristic);
                    L.d(TAG, address + " -- read result:" + result);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;

    }

    /**
     * 读取远程RSSI
     * @param address 蓝牙地址
     * @return 是否读取RSSI成功(这个是客户端的主观认为)
     */
    public boolean readRssi(String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        BluetoothGattCharacteristic gattCharacteristic = mReadCharacteristicMap.get(address);
        if (gattCharacteristic != null) {
            try {
                boolean result = mBluetoothGattMap.get(address).readRemoteRssi();
                L.d(TAG, address + " -- read result:" + result);
                return result;
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return false;

    }

    /**
     * 读取数据
     * @param address   蓝牙地址
     * @param characteristic 蓝牙特征对象
     */
    public void readCharacteristic(String address, BluetoothGattCharacteristic characteristic) {
        L.d(TAG, "readCharacteristic: " + characteristic.getProperties());
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).readCharacteristic(characteristic);
    }

    /**
     * 启用或禁用给定特征的通知
     *
     * @param address        蓝牙地址
     * @param characteristic 通知特征对象
     * @param enabled   是否设置通知使能
     */
    public void setCharacteristicNotification(String address,
                                              BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled);
        //If the number of descriptors in the eigenvalue of the notification is greater than zero
        if (characteristic.getDescriptors().size() > 0) {
            //Filter descriptors based on the uuid of the descriptor
            List<BluetoothGattDescriptor> descriptors = characteristic.getDescriptors();
            for(BluetoothGattDescriptor descriptor : descriptors){
                if (descriptor != null) {
                    //Write the description value
                    if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                    }else if((characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_INDICATION_VALUE);
                    }
                    mBluetoothGattMap.get(address).writeDescriptor(descriptor);
                }
            }
        }

    }

    /**
     * 设置通知数组
     * @param address 蓝牙地址
     * @param gattServices 蓝牙服务集合
     */
    private void displayGattServices(final String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null) return;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            String uuid = gattService.getUuid().toString();
            L.d(TAG, "displayGattServices: " + uuid);
            if (uuid.equals(mOptions.uuid_service.toString()) || isContainUUID(uuid)) {
                L.d(TAG, "service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    /*int charaProp = gattCharacteristic.getProperties();
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_READ) != 0) {
                        Log.e(TAG, "The readable UUID for gattCharacteristic is:" + gattCharacteristic.getUuid());
                        mReadCharacteristicMap.put(address, gattCharacteristic);
                    }
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_WRITE) != 0) {
                        Log.e(TAG, "The writable UUID for gattCharacteristic is:" + gattCharacteristic.getUuid());
                        mWriteCharacteristicMap.put(address, gattCharacteristic);
                    }
                    if ((charaProp & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        Log.e(TAG, "The PROPERTY_NOTIFY characteristic's UUID:" + gattCharacteristic.getUuid());
                        mNotifyCharacteristics.add(gattCharacteristic);
                    }
                    if((charaProp & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        Log.e(TAG, "The PROPERTY_INDICATE characteristic's UUID:" + gattCharacteristic.getUuid());
                        mNotifyCharacteristics.add(gattCharacteristic);
                    }*/
                    uuid = gattCharacteristic.getUuid().toString();
                    L.d(TAG, "Characteristic_uuid: " + uuid);
                    if (uuid.equals(mOptions.uuid_write_cha.toString())) {
                        L.e("mWriteCharacteristic", uuid);
                        mWriteCharacteristicMap.put(address, gattCharacteristic);
                        //Notification feature
                    } if (uuid.equals(mOptions.uuid_read_cha.toString())) {
                        L.e("mReadCharacteristic", uuid);
                        mReadCharacteristicMap.put(address, gattCharacteristic);
                        //Notification feature
                    } if ((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0) {
                        mNotifyCharacteristics.add(gattCharacteristic);
                        L.e("mNotifyCharacteristics", "PROPERTY_NOTIFY");
                    } if((gattCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0){
                        mNotifyCharacteristics.add(gattCharacteristic);
                        L.e("mNotifyCharacteristics", "PROPERTY_INDICATE");
                    }
                }
                //Really set up notifications
                if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0) {
                    L.e("setCharaNotification", "setCharaNotification");
                    setCharacteristicNotification(address, mNotifyCharacteristics.get(mNotifyIndex++), true);
                }
            }
        }
    }

    //是否包含该uuid
    private boolean isContainUUID(String uuid) {
        for (UUID u : mOptions.uuid_services_extra){
            if(u != null && uuid.equals(u.toString())){
                return true;
            }
        }
        return false;
    }

    /**
     * 获取可写特征对象
     * @param address 蓝牙地址
     * @return  可写特征对象
     */
    public BluetoothGattCharacteristic getWriteCharacteristic(String address) {
        synchronized (mLocker) {
            if (mWriteCharacteristicMap != null) {
                return mWriteCharacteristicMap.get(address);
            }
            return null;
        }
    }

    /**
     * 获取可读特征对象
     * @param address 蓝牙地址
     * @return  可读特征对象
     */
    public BluetoothGattCharacteristic getReadCharacteristic(String address) {
        synchronized (mLocker) {
            if (mReadCharacteristicMap != null) {
                return mReadCharacteristicMap.get(address);
            }
            return null;
        }
    }

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @param address ble address
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return null;

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * .读取连接的远程设备的RSSI
     *
     * @param address 蓝牙地址
     * @return 读取RSSI是否成功
     */
    public boolean getRssiVal(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return false;

        return mBluetoothGattMap.get(address).readRemoteRssi();
    }

    /**
     * 写入OTA数据
     *
     * @param address 蓝牙地址
     * @param value   发送字节数组
     * @return 写入是否成功
     */
    public boolean writeOtaData(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            L.w(TAG, address + " -- BluetoothAdapter not initialized");
            return false;
        }
        try {
            if (mOtaWriteCharacteristic == null) {
                mOtaUpdating = true;
                BluetoothGattService bluetoothGattService = this.mBluetoothGattMap.get(address).getService(mOptions.uuid_ota_service);
                if (bluetoothGattService == null) {
                    return false;
                } else {
                    BluetoothGattCharacteristic mOtaNotifyCharacteristic = bluetoothGattService.getCharacteristic(mOptions.uuid_ota_notify_cha);
                    if (mOtaNotifyCharacteristic != null) {
                        this.mBluetoothGattMap.get(address).setCharacteristicNotification(mOtaNotifyCharacteristic, true);
                    }
                    mOtaWriteCharacteristic = bluetoothGattService.getCharacteristic(mOptions.uuid_ota_write_cha);
                }

            }
            if (mOtaWriteCharacteristic != null && mOptions.uuid_ota_write_cha.equals(mOtaWriteCharacteristic.getUuid())) {
                mOtaWriteCharacteristic.setValue(value);
                boolean result = writeCharacteristic(mBluetoothGattMap.get(address), mOtaWriteCharacteristic);
                L.d(TAG, address + " -- write data:" + Arrays.toString(value));
                L.d(TAG, address + " -- write result:" + result);
                return result;
            }
            return true;
        } catch (Exception e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            close();
            return false;
        }
    }

    //The basic method of writing data
    public boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (mLocker) {
            return !(gatt == null || characteristic == null) && gatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * OTA升级完成
     */
    public void otaUpdateComplete() {
        mOtaUpdating = false;
    }

    /**
     * 设置OTA是否正在升级
     *
     * @param updating 升级状态
     */
    public void setOtaUpdating(boolean updating) {
        this.mOtaUpdating = updating;
    }

    /**
     * 设置OTA更新状态监听
     *
     * @param otaListener 监听对象
     */
    public void setOtaListener(OtaListener otaListener) {
        this.mOtaListener = otaListener;
    }


    public void setConnectWrapperLisenter(ConnectWrapperLisenter lisenter) {
        this.mConnectWrapperLisenter = lisenter;
    }

    public void setNotifyWrapperLisenter(NotifyWrapperLisenter lisenter) {
        this.mNotifyWrapperLisenter = lisenter;
    }

//    /**
//     * 蓝牙相关参数配置基类
//     */
//    public static class Options {
//
//        public UUID[] uuid_services_extra = new UUID[]{};
//        public UUID uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");
//        public UUID uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
//        public UUID uuid_read_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
//        public UUID uuid_notify = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");
//        public UUID uuid_notify_desc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");
//
//        public UUID uuid_ota_service = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb");
//        public UUID uuid_ota_notify_cha = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16");
//        public UUID uuid_ota_write_cha = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16");
//    }
}
