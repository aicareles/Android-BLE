package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 *
 * Created by LiuLei on 2017/10/21.
 */

public class ScanRequest<T extends BleDevice> {

    private boolean mScanning;
    private BluetoothAdapter mBluetoothAdapter;
    private BleScanCallback<T> mScanCallback;
    //    private AtomicBoolean isContains = new AtomicBoolean(false);
    private ArrayList<T> mScanDevices = new ArrayList<>();
    private static volatile ScanRequest instance;

    public static <T extends BleDevice> ScanRequest<T> getInstance(){
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


    public void startScan(BleScanCallback<T> callback, int scanPeriod) {
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

    private boolean constans(String address) {
        for (T device : mScanDevices) {
            if (device.getBleAddress().equals(address)) {
                return true;
            }
        }
        return false;
    }
}
