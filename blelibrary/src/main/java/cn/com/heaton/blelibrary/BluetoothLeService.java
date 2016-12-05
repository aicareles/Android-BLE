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


@SuppressLint("NewApi")
public class BluetoothLeService extends Service {

    private final static String TAG = BluetoothLeService.class.getSimpleName();

    private Handler mHandler;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    //public
    public boolean mScanning;

    private ArrayList<BluetoothDevice> mScanDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice> mConnectedDevices = new ArrayList<>();
    //Multiple device connections must put the gatt object in the collection
    private Map<String, BluetoothGatt> mBluetoothGattMap;
    //The address of the connected device
    private List<String> mConnectedAddressList;

    private Runnable                          mConnectTimeout        = new Runnable() { // 连接设备超时
        @Override
        public void run() {
            Toast.makeText(getApplication(),R.string.connect_timeout,Toast.LENGTH_SHORT).show();
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
            BleDevice bleDevice = new BleDevice(device);
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                bleDevice.setConnected(true);
                bleDevice.setConnectionState(BleConfig.CONNECTED);
                mBleLisenter.onConnectionChanged(gatt,bleDevice);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGattMap.get(device.getAddress()).discoverServices());

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                Log.i(TAG, "Disconnected from GATT server.");
                bleDevice.setConnected(false);
                bleDevice.setConnectionState(BleConfig.DISCONNECT);
                mBleLisenter.onConnectionChanged(gatt,bleDevice);
                close(device.getAddress());
            }
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleLisenter.onServicesDiscovered(gatt);
            } else {
                Log.w(TAG, "onServicesDiscovered received: " + status);
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic, int status) {
            Log.d(TAG, "onCharacteristicRead:" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                mBleLisenter.onRead(gatt.getDevice());
            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt,
                                          BluetoothGattCharacteristic characteristic, int status) {
            System.out.println("--------write success----- status:" + status);
            mBleLisenter.onWrite(gatt,characteristic,status);
        }


        /*
         * when connected successfully will callback this method , this method can dealwith send password or data analyze
         * When setnotify (true) is set, the method is called back if the data on the MCU (device side) changes.
         */
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
            mBleLisenter.onChanged(gatt, characteristic);
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt,
                                      BluetoothGattDescriptor descriptor, int status) {
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG,"onDescriptorWrite");
            Log.e(TAG,"descriptor_uuid:"+uuid);
            mBleLisenter.onDescriptorWriter(gatt);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorRead(gatt, descriptor, status);
            UUID uuid = descriptor.getCharacteristic().getUuid();
            Log.w(TAG,"onDescriptorRead");
            Log.e(TAG,"descriptor_uuid:"+uuid);
            mBleLisenter.onDescriptorRead(gatt);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
            System.out.println("rssi = " + rssi);
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        mHandler = new Handler();
    }

    /**
     * Starts scanning or stops scanning the device
     * @param enable  Whether to start
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
            mBleLisenter.onLeScan(device, rssi, scanRecord);
            mScanDevices.add(device);
        }
    };

//    /**
//     * Finds and returns the bluetooth device through Bluetooth address
//     * @param address
//     * @return
//     */
//    public BleDevice getBleDevice(String address){
//        return new BleDevice(address.)
//    }

    /**
     * Get the scanned device
     * @return
     */
    public List<BluetoothDevice> getScanDevices() {
        return mScanDevices;
    }

    /**
     * Gets the connected device
     * @return
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

    /**
     * Initializes the ble Bluetooth device
     * @return  Whether the initialization is successful
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
     * @param address
     * @return
     */
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
        mHandler.postDelayed(mConnectTimeout,BleConfig.CONNECT_TIME_OUT);
        if (mBluetoothGattMap.get(address) != null && mConnectedAddressList.contains(address)) {
            if (mBluetoothGattMap.get(address).connect()) {
                return true;
            } else {
                return false;
            }
        }

        final BluetoothDevice device = mBluetoothAdapter
                .getRemoteDevice(address);
        if (device == null) {
            Log.d(TAG, "没有设备");
            return false;
        }
        // We want to directly connect to the device, so we are setting the
        // autoConnect
        // parameter to false.
        BluetoothGatt bluetoothGatt = device.connectGatt(this, false, mGattCallback);
        if (bluetoothGatt != null) {
            mBluetoothGattMap.put(address, bluetoothGatt);
            Log.d(TAG, "Trying to create a new connection.");
            mConnectedAddressList.add(address);
            return true;
        }
        return false;
    }

    /**
     * Disconnects the specified Bluetooth blinking device
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattMap.get(address).disconnect();
    }

    /**
     *Clear the specified Bluetooth address of the Bluetooth bluetooth connection device
     */
    public void close(String address) {
        mConnectedAddressList.remove(address);
        if (mBluetoothGattMap.get(address) != null) {
            mBluetoothGattMap.get(address).close();
            mBluetoothGattMap.remove(address);
        }
    }

    /**
     *Clear all ble connected devices
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


    public boolean wirteCharacteristic(String address, BluetoothGattCharacteristic characteristic,byte[] value) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return false;
        }
        if(characteristic != null && BleConfig.UUID_CHARACTERISTIC.equals(characteristic.getUuid())){
            characteristic.setValue(value);
            boolean result = mBluetoothGattMap.get(address).writeCharacteristic(characteristic);
            Log.d(TAG, address + " -- write data:" + Arrays.toString(value));
            Log.d(TAG, address + " -- write result:" + result);
            return result;
        }
        return true;

    }

    /**
     * Request a read on a given {@code BluetoothGattCharacteristic}. The read
     * result is reported asynchronously through the
     * {@code BluetoothGattCallback#onCharacteristicRead(android.bluetooth.BluetoothGatt, android.bluetooth.BluetoothGattCharacteristic, int)}
     * callback.
     *
     * @param characteristic The characteristic to read from.
     */
    public void readCharacteristic(String address, BluetoothGattCharacteristic characteristic) {
        Log.d(TAG, "readCharacteristic: " + characteristic.getProperties());
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.d(TAG, "BluetoothAdapter为空");
            return;
        }
        mBluetoothGattMap.get(address).readCharacteristic(characteristic);
    }

    /**
     * Enables or disables notification on a give characteristic.
     *
     * @param characteristic Characteristic to act on.
     * @param enabled        If true, enable notification. False otherwise.
     */
    public void setCharacteristicNotification(String address,
                                              BluetoothGattCharacteristic characteristic, boolean enabled) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.d(TAG, "BluetoothAdapter为空");
            return;
        }
        mBluetoothGattMap.get(address).setCharacteristicNotification(characteristic, enabled);
        //If the number of descriptors in the eigenvalue of the notification is greater than zero
        if(characteristic.getDescriptors().size()>0){
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

    /**
     * Retrieves a list of supported GATT services on the connected device. This
     * should be invoked only after {@code BluetoothGatt#discoverServices()}
     * completes successfully.
     *
     * @return A {@code List} of supported services.
     */
    public List<BluetoothGattService> getSupportedGattServices(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return null;

        return mBluetoothGattMap.get(address).getServices();
    }

    /**
     * Read the RSSI for a connected remote device.
     */
    public boolean getRssiVal(String address) {
        if (mBluetoothGattMap.get(address) == null)
            return false;

        return mBluetoothGattMap.get(address).readRemoteRssi();
    }

    private BleLisenter mBleLisenter;

    public void setOnBleLisenter(BleLisenter bleLisenter) {
        mBleLisenter = bleLisenter;
    }

}
