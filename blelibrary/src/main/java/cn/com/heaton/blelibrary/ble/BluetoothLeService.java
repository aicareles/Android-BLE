package cn.com.heaton.blelibrary.ble;

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
import android.os.Message;
import android.util.Log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import cn.com.heaton.blelibrary.BuildConfig;
import cn.com.heaton.blelibrary.ota.OtaListener;


@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Handler mHandler;
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

    /**
     * Multiple device connections must put the gatt object in the collection
     */
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    /**
     * The address of the connected device
     */
    private List<String> mConnectedAddressList;


    private OtaListener mOtaListener;//Ota update operation listener

    /**
     * Connection changes or services were found in a variety of state callbacks
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            BluetoothDevice device = gatt.getDevice();
            //There is a problem here Every time a new object is generated that causes the same device to be disconnected and the connection produces two objects
//            if(status == BluetoothGatt.GATT_SUCCESS){
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    mIndex++;
                    mConnectedAddressList.add(device.getAddress());
///                    mHandler.removeCallbacks(mConnectTimeout);
                    mHandler.removeMessages(BleConfig.BleStatus.ConnectTimeOut);
                    mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 1, 0, device).sendToTarget();
                    Log.i(TAG, "Connected to GATT server.");
                    // Attempts to discover services after successful connection.
                    Log.i(TAG, "Attempting to start service discovery:"
                            + mBluetoothGattMap.get(device.getAddress()).discoverServices());

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
///                    mHandler.removeCallbacks(mConnectTimeout);
                    mHandler.removeMessages(BleConfig.BleStatus.ConnectTimeOut);
                    Log.i(TAG, "Disconnected from GATT server.");
                    mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 0, 0, device).sendToTarget();
                    close(device.getAddress());
                }
//            }else {
//                //出现133或者257  19等  值不为0：由于协议栈原因导致连接建立失败
//                Log.e(TAG, "onConnectionStateChange+++: "+"连接状态异常"+status);
//                mHandler.removeMessages(BleConfig.BleStatus.ConnectTimeOut);
//                mHandler.obtainMessage(BleConfig.BleStatus.ConnectFailed, device).sendToTarget();
//            }

        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mHandler.obtainMessage(BleConfig.BleStatus.ServicesDiscovered, gatt).sendToTarget();
                //Empty the notification attribute list
                mNotifyCharacteristics.clear();
                mNotifyIndex = 0;
                //Start setting notification feature
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
                mHandler.obtainMessage(BleConfig.BleStatus.Read, gatt.getDevice()).sendToTarget();
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
                    mHandler.obtainMessage(BleConfig.BleStatus.Write, gatt).sendToTarget();
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
                mHandler.obtainMessage(BleConfig.BleStatus.Changed, characteristic).sendToTarget();
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
                    }else {
                        Log.e(TAG, "====setCharacteristicNotification is true,ready to sendData===");
                        mHandler.obtainMessage(BleConfig.BleStatus.OnReady, gatt.getDevice()).sendToTarget();
                    }
                }
                mHandler.obtainMessage(BleConfig.BleStatus.DescriptorWriter, gatt).sendToTarget();
            }
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG, "onDescriptorRead");
            Log.e(TAG, "descriptor_uuid:" + uuid);
            mHandler.obtainMessage(BleConfig.BleStatus.DescriptorRead, gatt).sendToTarget();
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
    // TODO: 2017/6/6  connect
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

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "no device");
            return false;
        }
        //10s after the timeout prompt
        Message msg = Message.obtain();
        msg.what = BleConfig.BleStatus.ConnectTimeOut;
        msg.obj = device;
        mHandler.sendMessageDelayed(msg, BleConfig.getConnectTimeOut());
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false
        mHandler.obtainMessage(BleConfig.BleStatus.ConnectionChanged, 2, 0, device).sendToTarget();
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt != null) {
            mBluetoothGattMap.put(address, bluetoothGatt);
            Log.d(TAG, "Trying to create a new connection.");
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
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mNotifyIndex = 0;
        mBluetoothGattMap.get(address).disconnect();
        mNotifyCharacteristics.clear();
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
     * send data
     *
     * @param address ble address
     * @param value   Send data values
     * @return whether succeed
     */
    public boolean wirteCharacteristic(String address, byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
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
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(BleConfig.getUuidDescriptor());
            if (descriptor != null) {
                //Write the description value
                descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
                mBluetoothGattMap.get(address).writeDescriptor(descriptor);
            }
        }

    }

    //Set the notification array
    private void displayGattServices(final String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices: " + uuid);
            if (uuid.equals(BleConfig.getUuidService().toString())) {
                Log.d(TAG, "service_uuid: " + uuid);
                List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
                for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                    uuid = gattCharacteristic.getUuid().toString();
                    if (uuid.equals(BleConfig.getUuidCharacteristic().toString())) {
                        Log.e("mWriteCharacteristic", uuid);
                        mWriteCharacteristicMap.put(address,gattCharacteristic);
                        //Notification feature
                    } else if (gattCharacteristic.getProperties() == BluetoothGattCharacteristic.PROPERTY_NOTIFY) {
                        mNotifyCharacteristics.add(gattCharacteristic);
                        Log.e("mNotifyCharacteristics", "PROPERTY_NOTIFY");
                    }
//                    uuid = gattCharacteristic.getUuid().toString();
//                    Log.e(TAG,"all_characteristic: " + uuid);
//                    if (uuid.equals(BleConfig.UUID_NOTIFY_TEXT)) {
//                        Log.e(TAG,"2gatt Characteristic: " + uuid);
//                        setCharacteristicNotification(address, gattCharacteristic, true);
////                        mBluetoothLeService.readCharacteristic(address,gattCharacteristic);
//                    } else if (uuid.equals(BleConfig.UUID_CHARACTERISTIC_TEXT)) {
//                        Log.e(TAG,"write_characteristic: " + uuid);
//                    }

                }
                //Really set up notifications
                if (mNotifyCharacteristics != null && mNotifyCharacteristics.size() > 0) {
                    Log.e("setCharaNotification", "setCharaNotification");
                    setCharacteristicNotification(address, mNotifyCharacteristics.get(mNotifyIndex++), true);
                }
            }
        }
    }

    //Get a writable WriteCharacteristic object
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
     * Send ota data
     *
     * @param address Device address
     * @param value  Send data values
     * @return whether succeed
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

    //The basic method of writing data
    public boolean writeCharacteristic(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
        synchronized (mLocker) {
            if (gatt == null || characteristic == null) {
                return false;
            }
            return gatt.writeCharacteristic(characteristic);
        }
    }

    /**
     * update completed
     */
    public void otaUpdateComplete() {
        mOtaUpdating = false;
    }

    /**
     * Set whether ota is updated
     *
     * @param updating update status
     */
    public void setOtaUpdating(boolean updating) {
        mOtaUpdating = updating;
    }

    /**
     * OTA sets the listener
     *
     * @param otaListener Listener
     */
    public void setOtaListener(OtaListener otaListener) {
        mOtaListener = otaListener;
    }


}
