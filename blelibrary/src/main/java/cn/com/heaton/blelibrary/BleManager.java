package cn.com.heaton.blelibrary;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
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
import java.util.UUID;

import cn.com.heaton.blelibrary.BleVO.BleDevice;

/**
 * This class provides various APIs for Bluetooth operation
 * Created by liulei on 2016/12/7.
 */

public class BleManager {

    private final static String TAG = "BleManager";
    public static final int REQUEST_ENABLE_BT = 1;
    private Context mContext;
    private BluetoothLeService mBluetoothLeService;
    private static BleLisenter mBleLisenter;
    public boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private ArrayList<BluetoothDevice> mScanDevices = new ArrayList<>();
    private static BleManager instance;

    private Handler mHandler = new Handler(Looper.getMainLooper()){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case BleConfig.ConnectTimeOut:
                    mBleLisenter.onConnectTimeOut();
                    break;
                case BleConfig.ConnectionChanged:
                    mBleLisenter.onConnectionChanged((BleDevice) msg.obj);
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

    public static BleManager getInstance(Context context,BleLisenter bleLisenter) throws Exception{
        mBleLisenter = bleLisenter;
        if(instance == null){
            synchronized (BleManager.class){
                if(instance == null){
                    instance = new BleManager(context);
                }
            }
        }
        if(instance.isSupportBle()){
            return instance;
        }else {
            throw new Exception("BLE is not supported");
        }
    }

    /**
     * Whether to support Bluetooth
     * @return Whether to support Ble
     */
    public boolean isSupportBle(){
        return (mBluetoothAdapter != null && mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE));
    }

    public BluetoothLeService getBleService(){
        return mBluetoothLeService;
    }
    /**
     * Bluetooth is turned on
     * @return true  Bluetooth is turned on
     */
    public boolean isBleEnable() {
        return mBluetoothAdapter.isEnabled();
    }

    /**
     * open ble
     * @param activity The context object
     */
    public void turnOnBlueTooth(Activity activity) {
        // Ensures Bluetooth is enabled on the device.  If Bluetooth is not currently enabled,
        // fire an intent to display a dialog asking the user to grant permission to enable it.
        if (!isBleEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }
        //打开成功的回调在onActivityResult方法中：resultCode == RESULT_OK则打开成功
    }

    /**
     * close ble
     */
    public boolean turnOffBlueTooth() {
        return !mBluetoothAdapter.isEnabled() || mBluetoothAdapter.disable();
    }

    /**
     * start bind service
     * @return Whether the service is successfully bound
     */
    public boolean startService() {
        Intent gattServiceIntent = new Intent(mContext, BluetoothLeService.class);
        boolean bll = false;
        if(mContext != null){
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
    public void unService(){
        if(mContext != null){
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
                if(mBleLisenter != null){
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

    /**
     * Get the scanned device
     * @return scanned device
     */
    public List<BluetoothDevice> getScanDevices() {
        return mScanDevices;
    }

    /**
     * Gets the connected device
     * @return  connected device
     */
    public List<BluetoothDevice> getConnectedDevices() {
        if(mBluetoothLeService != null){
            return mBluetoothLeService.getConnectedDevices();
        }
        return null;
    }

    /**
     * connecte bleDevice
     * @param address ble address
     */
    public void connect(String address){
        if(mBluetoothLeService != null){
            mBluetoothLeService.connect(address);
        }
    }

    /**
     * disconnect device
     * @param address  ble address
     */
    public void disconnect(String address) {
        if(mBluetoothLeService != null){
            mBluetoothLeService.disconnect(address);
        }
    }

    /**
     * Set up notifications
     * @param address ble address
     * @param characteristic Bluetooth device object
     * @param enabled Whether to set notifications
     */
    public void setCharacteristicNotification(String address, BluetoothGattCharacteristic characteristic, boolean enabled) {
        if(mBluetoothLeService != null){
            mBluetoothLeService.setCharacteristicNotification(address,characteristic, true);
        }
    }

    /**
     * send data
     * @param address ble address
     * @param characteristic Bluetooth device object
     * @param data data
     * @return Whether send success
     */
    public boolean sendData(String address, BluetoothGattCharacteristic characteristic, byte[]data){
        boolean result = false;
        if(mBluetoothLeService != null){
            result = mBluetoothLeService.wirteCharacteristic(address, characteristic, data);
        }
        return result;
    }

























}
