package cn.com.heaton.blelibrary;

import android.annotation.SuppressLint;
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
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.com.heaton.blelibrary.BleVO.BleDevice;
import cn.com.heaton.blelibrary.ota.OtaListener;


@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Handler mHandler;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private final Object mLocker = new Object();
//    private BluetoothGattCharacteristic mWriteCharacteristic;//可写的GattCharacteristic对象
    private List<BluetoothGattCharacteristic> mNotifyCharacteristics = new ArrayList<>();//通知特性回调数组
    private int mNotifyIndex = 0;//通知特性回调列表
    private int mIndex = 0;//设备索引
    private BluetoothGattCharacteristic mOtaWriteCharacteristic;//ota ble发送对象
    private boolean mOtaUpdating = false;//是否OTA更新

    private Map<String, BluetoothGattCharacteristic> mWriteCharacteristicMap = new HashMap<>();

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    /**
     * The address of the connected device
     */
    private List<String> mConnectedAddressList;

    //当前正在连接的设备
    private BluetoothDevice currentDevice = null;

    private OtaListener mOtaListener;//ota更新操作监听器

    private Runnable mConnectTimeout = new Runnable() { // 连接设备超时
        @Override
        public void run() {
            mHandler.sendEmptyMessage(BleConfig.ConnectTimeOut);
            if (currentDevice != null) {
                disconnect(currentDevice.getAddress());
                close(currentDevice.getAddress());
                mHandler.obtainMessage(BleConfig.ConnectionChanged, 0, 0, currentDevice).sendToTarget();
            }
        }
    };

    /**
     * Connection changes or services were found in a variety of state callbacks
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            BluetoothDevice device = gatt.getDevice();
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects

            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mIndex++;
                mConnectedAddressList.add(device.getAddress());//连接成功之后  添加
                mHandler.removeCallbacks(mConnectTimeout);
                mHandler.obtainMessage(BleConfig.ConnectionChanged, 1, 0, device).sendToTarget();
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGattMap.get(device.getAddress()).discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                Log.i(TAG, "Disconnected from GATT server.");
                mHandler.obtainMessage(BleConfig.ConnectionChanged, 0, 0, device).sendToTarget();
                close(device.getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleConfig.ServicesDiscovered, gatt).sendToTarget();
                //清空通知特性列表
                mNotifyCharacteristics.clear();
                mNotifyIndex = 0;
                //开始设置通知特性
                displayGattServices(gatt.getDevice().getAddress(), getSupportedGattServices(gatt.getDevice().getAddress()));
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleConfig.Read, gatt.getDevice()).sendToTarget();
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("--------write success----- status:" + status);
            synchronized (mLocker) {
                if(BuildConfig.DEBUG){
                    Log.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicWrite: " + status);
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (BleConfig.UUID_OTA_WRITE_CHARACTERISTIC.equals(characteristic.getUuid())) {
                        if (mOtaListener != null) {
                            mOtaListener.onWrite();
                        }
                        return;
                    }
                    mHandler.obtainMessage(BleConfig.Write, gatt).sendToTarget();
                }
            }
        }


        /*
         * when connected successfully will callback this method , this method can dealwith send password or data analyze
         * When setnotify (true) is set, the method is called back if the data on the MCU (device side) changes.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            synchronized (mLocker) {
                if(BuildConfig.DEBUG){
                    Log.i(TAG, gatt.getDevice().getAddress() + " -- onCharacteristicWrite: " + (characteristic.getValue() != null ? Arrays.toString(characteristic.getValue()) : ""));
                }
                if (BleConfig.UUID_OTA_WRITE_CHARACTERISTIC.equals(characteristic.getUuid()) || BleConfig.UUID_OTA_NOTIFY_CHARACTERISTIC.equals(characteristic.getUuid())) {
                    if (mOtaListener != null) {
                        mOtaListener.onChange(characteristic.getValue());
                    }
                    return;
                }
                mHandler.obtainMessage(BleConfig.Changed, characteristic).sendToTarget();
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG, "onDescriptorWrite");
            Log.e(TAG, "descriptor_uuid:" + uuid);
            synchronized (mLocker) {
                if (BuildConfig.DEBUG) {
                    Log.e(TAG, " -- onDescriptorWrite: " + status);
                }
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0 && mNotifyIndex < mNotifyCharacteristics.size()) {
                        setCharacteristicNotification(gatt.getDevice().getAddress(), mNotifyCharacteristics.get(mNotifyIndex++), true);
                    }
                }
                mHandler.obtainMessage(BleConfig.DescriptorWriter, gatt).sendToTarget();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG, "onDescriptorRead");
            Log.e(TAG, "descriptor_uuid:" + uuid);
            mHandler.obtainMessage(BleConfig.DescriptorRead, gatt).sendToTarget();
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("rssi = " + rssi);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
    }

//    /**
//     * Finds and returns the bluetooth device through Bluetooth address
//     * @param address
//     * @return
//     */
//    public BleDevice getBleDevice(String address){
//        return new BleDevice(address.)
//    }

    /**
     * Gets the connected device
     *
     * @return connected device
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
        return mBinder;
    }

    @Override
    public boolean onUnbind(Intent intent) {
        close();
        return super.onUnbind(intent);
    }

    private final IBinder mBinder = new LocalBinder();

    public void setHandler(Handler handler) {
        mHandler = handler;
    }

    public Handler getHandler() {
        return mHandler;
    }

    /**
     * Initializes the ble Bluetooth device
     *
     * @return Whether the initialization is successful
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        //Bluetooth 4.0, that API level> = 18, and supports Bluetooth 4.0 phone can use, if the mobile phone system version API level <18, is not used Bluetooth 4 android system 4.3 above, the phone supports Bluetooth 4.0
        if (mBluetoothManager == null) {
            mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            if (mBluetoothManager == null) {
                Log.e(TAG, "Unable to initialize BluetoothManager.");
                return false;
            }
        }

        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Log.e(TAG, "Unable to obtain a BluetoothAdapter.");
            return false;
        }

        return true;
    }

    /**
     * Connects to a specified Bluetooth device
     *
     * @param address ble address
     * @return Whether connect is successful
     */
    // TODO: 2017/6/6  连接设备 
    public boolean connect(final String address) {

        if (mConnectedAddressList == null) {
            mConnectedAddressList = new ArrayList<>();
        }
        if (mConnectedAddressList.contains(address)) {
            Log.d(TAG, "This is device already connected.");
            return true;
        }

        if (mBluetoothAdapter == null || address == null) {
            Log.w(TAG,
                    "BluetoothAdapter not initialized or unspecified address.");
            return false;
        }
        // Previously connected device. Try to reconnect. ()
        if (mBluetoothGattMap == null) {
            mBluetoothGattMap = new HashMap<>();
        }
        //10s after the timeout prompt
        mHandler.postDelayed(mConnectTimeout, BleConfig.CONNECT_TIME_OUT);
//        if (mBluetoothGattMap.get(address) != null && mConnectedAddressList.contains(address)) {
//            if (mBluetoothGattMap.get(address).connect()) {
//                return true;
//            } else {
//                return false;
//            }
//        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "no device");
            return false;
        }
        currentDevice = device;
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false
        mHandler.obtainMessage(BleConfig.ConnectionChanged, 2, 0, device).sendToTarget();
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt != null) {
            mBluetoothGattMap.put(address, bluetoothGatt);
            Log.d(TAG, "Trying to create a new connection.");
//            mConnectedAddressList.add(address);//暂时注释
            return true;
        }
        return false;
    }

    /**
     * Disconnects the specified Bluetooth blinking device
     *
     * @param address ble address
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.e(TAG, mBluetoothGattMap.get(address).getDevice().getAddress());
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mNotifyIndex = 0;
        mBluetoothGattMap.get(address).disconnect();
        mNotifyCharacteristics.clear();
//        mWriteCharacteristic = null;
        mWriteCharacteristicMap.remove(address);
        mOtaWriteCharacteristic = null;
    }

    /**
     * Clear the specified Bluetooth address of the Bluetooth bluetooth connection device
     *
     * @param address ble address
     */
    public void close(String address) {
        mConnectedAddressList.remove(address);
        if (mBluetoothGattMap.get(address) != null) {
            mBluetoothGattMap.get(address).close();
            mBluetoothGattMap.remove(address);
        }
    }

    /**
     * Clear all ble connected devices
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
     * 发送数据
     *
     * @param address 发送对象
     * @param value   发送数据值
     * @return 是否成功
     */
    public boolean wirteCharacteristic(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
//        try {
//            if (mWriteCharacteristic != null && BleConfig.UUID_CHARACTERISTIC.equals(mWriteCharacteristic.getUuid())) {
//                mWriteCharacteristic.setValue(value);
//                boolean result = mBluetoothGattMap.get(address).writeCharacteristic(mWriteCharacteristic);
//                Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
//                Log.d(TAG, address + " -- write result:" + result);
//                return result;
//            }
//        }
        BluetoothGattCharacteristic gattCharacteristic = mWriteCharacteristicMap.get(address);
        if(gattCharacteristic != null){
            try {
                if (BleConfig.UUID_CHARACTERISTIC.equals(gattCharacteristic.getUuid())) {
                    gattCharacteristic.setValue(value);
                    boolean result = mBluetoothGattMap.get(address).writeCharacteristic(gattCharacteristic);
                    Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
                    Log.d(TAG, address + " -- write result:" + result);
                    return result;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
//        if(characteristic != null && BleConfig.UUID_CHARACTERISTIC.equals(characteristic.getUuid())){
//            characteristic.setValue(value);
//            boolean result = mBluetoothGattMap.get(address).writeCharacteristic(characteristic);
//            Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
//            Log.d(TAG, address + " -- write result:" + result);
//            return result;
//        }
        return false;

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param address        ble address
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(String address, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "readCharacteristic: " + characteristic.getProperties());
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param address        ble address
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(String address,
                                              BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.d(TAG, "BluetoothAdapter is null");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled);
        //If the number of descriptors in the eigenvalue of the notification is greater than zero
        if (characteristic.getDescriptors().size() > 0) {
            //Filter descriptors based on the uuid of the descriptor
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID
                    .fromString(BleConfig.UUID_DESCRIPTOR_TEXT));
            if (descriptor != null) {
                //Write the description value
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGattMap.get(address).writeDescriptor(descriptor);
            }
        }

    }

    //设置通知数组
    private void displayGattServices(String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices: " + uuid);
            if (uuid.equals(BleConfig.UUID_SERVICE_TEXT)) {
                Log.d(TAG, "service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(BleConfig.UUID_CHARACTERISTIC_TEXT)) {
                        Log.e("mWriteCharacteristic", uuid);
//                        mWriteCharacteristic = gattCharacteristic;
                        mWriteCharacteristicMap.put(address,gattCharacteristic);
                        //通知特性
                    } else if (gattCharacteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                        mNotifyCharacteristics.add(gattCharacteristic);
                        Log.e("mNotifyCharacteristics", "PROPERTY_NOTIFY");
                    }
                    if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0) {
                        Log.e("setCharaNotification", "setCharaNotification");
                        setCharacteristicNotification(address, mNotifyCharacteristics.get(mNotifyIndex++), true);
                    }
//                    uuid = gattCharacteristic.getUuid().toString();
//                    Log.e(TAG,"all_characteristic: " + uuid);
//                    if (uuid.equals(BleConfig.UUID_NOTIFY_TEXT)) {
//                        Log.e(TAG,"2gatt Characteristic: " + uuid);
//                        setCharacteristicNotification(address, gattCharacteristic, true);
////                        mBluetoothLeService.readCharacteristic(address,gattCharacteristic);//暂时注释
//                    } else if (uuid.equals(BleConfig.UUID_CHARACTERISTIC_TEXT)) {
//                        Log.e(TAG,"write_characteristic: " + uuid);
//                    }

                }
            }
        }
    }

    //获取可写的WriteCharacteristic对象
    public BluetoothGattCharacteristic getWriteCharacteristic(String address) {
        synchronized (mLocker) {
            if (mWriteCharacteristicMap != null) {
                return  mWriteCharacteristicMap.get(address);
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
     * Read the RSSI for a connected remote device.
     *
     * @param address ble address
     * @return Whether get rssi values is successful
     */
    public boolean getRssiVal(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return false;

        return mBluetoothGattMap.get(address).readRemoteRssi();
    }

    /**
     * 发送ota数据
     *
     * @param address 设备地址
     * @param value   数据对象
     * @return 发送结果
     */
    public boolean writeOtaData(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            if (BuildConfig.DEBUG) {
                Log.w(TAG, address + " -- BluetoothAdapter not initialized");
            }
            return false;
        }
        try {
            if (mOtaWriteCharacteristic == null) {
                mOtaUpdating = true;
                BluetoothGattService bluetoothGattService = this.mBluetoothGattMap.get(address).getService(BleConfig.UUID_QUINTIC_OTA_SERVICE);
                if (bluetoothGattService == null) {
                    return false;
                } else {
                    BluetoothGattCharacteristic mOtaNotifyCharacteristic = bluetoothGattService.getCharacteristic(BleConfig.UUID_OTA_NOTIFY_CHARACTERISTIC);
                    if (mOtaNotifyCharacteristic != null) {
                        this.mBluetoothGattMap.get(address).setCharacteristicNotification(mOtaNotifyCharacteristic, true);
                    }
                    mOtaWriteCharacteristic = bluetoothGattService.getCharacteristic(BleConfig.UUID_OTA_WRITE_CHARACTERISTIC);
                }

            }
            if (mOtaWriteCharacteristic != null && BleConfig.UUID_OTA_WRITE_CHARACTERISTIC.equals(mOtaWriteCharacteristic.getUuid())) {
                mOtaWriteCharacteristic.setValue(value);
                boolean result = writeCharacteristic(mBluetoothGattMap.get(address), mOtaWriteCharacteristic);
                if (BuildConfig.DEBUG) {
                    Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
                    Log.d(TAG, address + " -- write result:" + result);
                }
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

    //写入数据的基本方法
    public boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (mLocker) {
            if (gatt == null || characteristic == null) {
                return false;
            }
            return gatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * 更新完成
     */
    public void otaUpdateComplete() {
        mOtaUpdating = false;
    }

    /**
     * 设置是否ota更新
     *
     * @param updating 更新状态
     */
    public void setOtaUpdating(boolean updating) {
        mOtaUpdating = updating;
    }

    /**
     * OTA设置监听器
     *
     * @param otaListener 监听器对象
     */
    public void setOtaListener(OtaListener otaListener) {
        mOtaListener = otaListener;
    }


}
