package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.ArrayList;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */

public class ScanRequest<T> {

    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private BleScanCallback<BleDevice> mScanCallback;
    //    private AtomicBoolean isContains = new AtomicBoolean(false);
    private ArrayList<BleDevice> mScanDevices = new ArrayList<>();
    private static volatile ScanRequest instance;

    public static ScanRequest getInstance(){
        if (instance == null) {
            synchronized (ScanRequest.class) {
                if (instance == null) {
                    instance = new ScanRequest();
                }
            }
        }
        return instance;
    }

    private ScanRequest() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }


    public void startScan(BleScanCallback<BleDevice> callback, int scanPeriod) {
        mScanCallback = callback;
        // Stops scanning after a pre-defined scan period.
        BleHandler.getHandler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mScanning = false;
                mBluetoothAdapter.stopLeScan(mLeScanCallback);
                mScanCallback.onStop();
            }
        }, scanPeriod);

        mScanning = true;
        mBluetoothAdapter.startLeScan(mLeScanCallback);
        mScanCallback.onStart();
    }

    public void stopScan() {
        if (mScanning) {
            mScanning = false;
            mBluetoothAdapter.stopLeScan(mLeScanCallback);
            mScanDevices.clear();
        }
    }

    public boolean isScanning() {
        return mScanning;
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            if (device == null) return;
            if (TextUtils.isEmpty(device.getName())) return;
            if (!constans(device.getAddress())) {
                BleDevice bleDevice = new BleDevice(device);
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
//                            //说明非主动断开设备   理论上需要自动重新连接（前提是连接时设置自动连接属性为true）
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

    private boolean constans(String address) {
        for (BleDevice device : mScanDevices) {
            if (device.getBleAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }
}
