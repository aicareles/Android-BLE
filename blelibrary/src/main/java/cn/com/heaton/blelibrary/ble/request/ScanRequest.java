package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Message;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;

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
    private BLEScanCallback mScannerCallback;
    private List<ScanFilter> mFilters;
    //    private AtomicBoolean isContains = new AtomicBoolean(false);
    private ArrayList<T> mScanDevices = new ArrayList<>();
    private ConnectRequest mConnectRequest;

    protected ScanRequest() {
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BleHandler.of().setHandlerCallback(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
            //mScanner will be null if Bluetooth has been closed
            mScanner = mBluetoothAdapter.getBluetoothLeScanner();
            mScannerSetting = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            mScannerCallback  = new BLEScanCallback();
            mFilters = new ArrayList<>();
        }
    }

    public void startScan(BleScanCallback<T> callback, long scanPeriod) {
        if(mScanning)return;
        if(callback != null){
            mScanCallback = callback;
        }
        mScanning = true;
        // Stops scanning after a pre-defined scan period.
        BleHandler.of().postDelayed(new Runnable() {
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
            if (mBluetoothAdapter.isEnabled()) {
                if (mScanner == null) {
                    mScanner = mBluetoothAdapter.getBluetoothLeScanner();
                }
                /*byte[] manufacture = {0x00, 0x2A};
                mFilters.add(new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString("0000ae00-0000-1000-8000-00805f9b34fb"))
                        .setManufacturerData(0x5254, manufacture)
                        .build());*/
                mScanner.startScan(mFilters, mScannerSetting, mScannerCallback);
            }
        }
        if(callback != null){
            mScanCallback.onStart();
        }
    }

    public void stopScan() {
        if (!mScanning) return;
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
        if(mScanCallback != null){
            mScanCallback.onStop();
        }
    }

    public boolean isScanning() {
        return mScanning;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private class BLEScanCallback extends ScanCallback {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            T bleDevice = dispatcherScanResult(device, result.getRssi(), scanRecord);
            ScanRecord parseRecord = ScanRecord.parseFromBytes(scanRecord);
            if (parseRecord != null && mScanCallback != null){
                mScanCallback.onParsedData(bleDevice, parseRecord);
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
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            dispatcherScanResult(device, rssi, scanRecord);
        }
    };

    private T dispatcherScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) return null;
        T bleDevice = getDevice(device.getAddress());
        if (bleDevice == null) {
            bleDevice = (T) BleFactory.create(BleDevice.class, device);
            if(mScanCallback != null){
                mScanCallback.onLeScan(bleDevice, rssi, scanRecord);
            }
            mScanDevices.add(bleDevice);
        }else {
            if (!Ble.options().isFilterScan){//无需过滤
                if(mScanCallback != null){
                    mScanCallback.onLeScan(bleDevice, rssi, scanRecord);
                }
            }
        }
//        //自动重连
//        autoConnect(device);
        return bleDevice;
    }

    /*private void autoConnect(BluetoothDevice device){
        if (mConnectRequest == null){
            mConnectRequest = Rproxy.getInstance().getRequest(ConnectRequest.class);
        }
        mConnectRequest.autoConnect(device);
    }*/

    //获取已扫描到的设备（重复设备）
    private T getDevice(String address) {
        for (T device : mScanDevices) {
            if (device.getBleAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case BleStates.BleStatus.BlutoothStatusOff:
                if(mScanning){
                    stopScan();
                }
                break;
        }
    }
}
