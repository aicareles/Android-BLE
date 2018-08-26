package cn.com.heaton.blelibrary.ble.request;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */
@Implement(ScanRequest.class)
public class ScanRequest<T extends BleDevice> implements IMessage {

    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothLeScanner mScanner;
    private ScanSettings mScannerSetting;
    private BleScanCallback<T> mScanCallback;
    private List<ScanFilter> mFilters;
    //    private AtomicBoolean isContains = new AtomicBoolean(false);
    private ArrayList<T> mScanDevices = new ArrayList<>();
    private Ble<T> mBle;

    protected ScanRequest() {
        mBle = Ble.getInstance();
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //mScanner will be null if Bluetooth has been closed
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScannerSetting = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mFilters = new ArrayList<>();
        }
    }

    public synchronized void startScan(BleScanCallback<T> callback, int scanPeriod) {
        if(mScanning) {
            return;
        }
        mScanCallback = callback;
        mScanning = true;
        // Stops scanning after a pre-defined scan period.
        BleHandler.getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if(mScanning){
                    stopScan();
                }
            }
        }, scanPeriod);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            mBluetoothAdapter.startLeScan(mLeScanCallback);
        } else {
            //the status of bluetooth will be checked in the original codes of startScan().
            //once bluetooth is closed,it will throw an exception, so to avoid this, it's
            //necessary to check the status of bluetooth before calling startScan()
            if (mBluetoothAdapter.isEnabled()) {
                //mScanner may be null when it was initialized without opening bluetooth, so recheck it
                if (mScanner == null) {
                    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                mScanner.startScan(mFilters, mScannerSetting, mScannerCallback);
            }
        }
        mScanCallback.onStart();
    }

    public synchronized void stopScan() {
        if (!mScanning) {
            return;
        }
        mScanning = false;
        if(Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP){
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
        }else {
            if (mBluetoothAdapter.isEnabled()) {
                if (mScanner == null) {
                    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                mScanner.stopScan(mScannerCallback);
            }
        }
        mScanDevices.clear();
        mScanCallback.onStop();
    }

    public synchronized boolean isScanning() {
        return mScanning;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback mScannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            if (device == null) return;
            if (TextUtils.isEmpty(device.getName())) return;
            L.i("onScanResult==deviceName:", result.getDevice().getName());
            if (!constains(device.getAddress())) {
                T bleDevice = (T) BleFactory.create(BleDevice.class,  Ble.getInstance(), device);
                mScanCallback.onLeScan(bleDevice, result.getRssi(), scanRecord);
                mScanDevices.add(bleDevice);
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                L.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            L.e("Scan Failed", "Error Code: " + errorCode);
        }
    };


    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            if (device == null) return;
            if (TextUtils.isEmpty(device.getName())) return;
            if (!constains(device.getAddress())) {
                T bleDevice = (T) BleFactory.create(BleDevice.class,  Ble.getInstance(), device);
                mScanCallback.onLeScan(bleDevice, rssi, scanRecord);
                mScanDevices.add(bleDevice);
            }
//            if (!contains(device)) {
//                T bleDevice = (T) new BleDevice(device);
//                for (BleLisenter bleLisenter : mBleLisenters) {
//                    bleLisenter.onLeScan(bleDevice, rssi, scanRecord);
//                }
//                mScanDevices.add(bleDevice);
//            } else {
//                synchronized (mLocker) {
//                    for (T autoDevice : mAutoDevices) {
//                        if (device.getAddress().equals(autoDevice.getBleAddress())) {
//                            //Note non-active disconnect device in theory need to re-connect automatically (provided the connection is set to automatically connect property is true)
//                            if (!autoDevice.isConnected() && !autoDevice.isConnectting() && autoDevice.isAutoConnect()) {
//                                Log.e(TAG, "onLeScan: " + "正在重连设备...");
//                                reconnect(autoDevice);
//                                mAutoDevices.remove(autoDevice);
//                            }
//                        }
//                    }
//                }
//            }
        }
    };

    private void autoConnect(BluetoothDevice device){
        T b = mBle.getBleDevice(device);
        if(b != null && b.getConnectionState() == BleStates.BleStatus.DISCONNECT && b.isAutoConnect()){
            mBle.reconnect(b);
        }

    }

    private boolean constains(String address) {
        for (T device : mScanDevices) {
            if (device.getBleAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handleMessage(Message msg) {

    }
}
