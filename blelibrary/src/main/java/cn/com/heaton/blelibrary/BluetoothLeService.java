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
    public boolean mScanning;//公共的

    private ArrayList<BluetoothDevice> mScanDevices = new ArrayList<>();
    private ArrayList<BluetoothDevice> mConnectedDevices = new ArrayList<>();
    private Map<String, BluetoothGatt> mBluetoothGattMap;//多设备连接  必须要把gatt对象放进集合中
    private List<String> mConnectedAddressList;//已连接设备的address

    private Runnable                          mConnectTimeout        = new Runnable() { // 连接设备超时
        @Override
        public void run() {
            Toast.makeText(getApplication(),R.string.connect_timeout,Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * 连接改变或者服务被发现等多种状态的回调
     */
    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                                            int newState) {
            BluetoothDevice device = gatt.getDevice();
            BleDevice bleDevice = new BleDevice(device);//这里有问题  每次都生成一个新的对象  导致同一个设备断开和连接   产生了两个对象
            if (newState == BluetoothProfile.STATE_CONNECTED) {
                mHandler.removeCallbacks(mConnectTimeout);
                bleDevice.setConnected(true);
                bleDevice.setConnectionState(BleConfig.CONNECTED);
                mBleLisenter.onConnectionChanged(gatt,bleDevice);
                Log.i(TAG, "Connected to GATT server.");
                // Attempts to discover services after successful connection.
                Log.i(TAG, "Attempting to start service discovery:"
                        + mBluetoothGattMap.get(device.getAddress()).discoverServices());//mBluetoothGatt.discoverServices()

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
         * 当设置了setnotify（true）时,如果muc（设备端）有数据发生变化时会回调该方法
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
     * 开始扫描或者停止扫描设备
     * @param enable  是否开始
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
//     * 通过蓝牙地址查找并返回ble蓝牙设备
//     * @param address
//     * @return
//     */
//    public BleDevice getBleDevice(String address){
//        return new BleDevice(address.)
//    }

    /**
     * 获取扫描到的设备
     * @return
     */
    public List<BluetoothDevice> getScanDevices() {
        return mScanDevices;
    }

    /**
     * 获取已连接的设备
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
     * 初始化ble蓝牙设备
     * @return  是否初始化成功
     */
    public boolean initialize() {
        // For API level 18 and above, get a reference to BluetoothAdapter
        //蓝牙4.0，也就是说API level >= 18，且支持蓝牙4.0的手机才可以使用，如果手机系统版本API level < 18，也是用不了蓝牙4.0的  android系统4.3以上，手机支持蓝牙4.0
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
     * 连接指定address的ble蓝牙设备
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
        mHandler.postDelayed(mConnectTimeout,BleConfig.CONNECT_TIME_OUT);//10s后超时提示
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
     * 断开指定address的ble蓝牙连接设备
     */
    public void disconnect(final String address) {
        if (mBluetoothAdapter == null || mBluetoothGattMap.get(address) == null) {
            Log.w(TAG, "BluetoothAdapter not initialized");
            return;
        }
        mBluetoothGattMap.get(address).disconnect();
    }

    /**
     *清除指定address蓝牙地址的ble蓝牙连接设备
     */
    public void close(String address) {
        mConnectedAddressList.remove(address);
        if (mBluetoothGattMap.get(address) != null) {
            mBluetoothGattMap.get(address).close();
            mBluetoothGattMap.remove(address);
        }
    }

    /**
     *清除所有的ble蓝牙连接设备
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
        if(characteristic.getDescriptors().size()>0){//如果通知的特征值中的描述符个数大于0
            BluetoothGattDescriptor descriptor = characteristic.getDescriptor(UUID  //根据描述符的uuid进行过滤描述符
                    .fromString(BleConfig.UUID_DESCRIPTOR_TEXT));
            if (descriptor != null) {
                //写入描述值
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
