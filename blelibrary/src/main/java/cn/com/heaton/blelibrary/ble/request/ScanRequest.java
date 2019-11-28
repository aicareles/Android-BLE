package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.Handler;
import android.os.ParcelUuid;
import android.support.annotation.RequiresApi;
import android.support.v4.os.HandlerCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleFactory;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.BleUtils;

/**
 * Created by LiuLei on 2017/10/21.
 */
@Implement(ScanRequest.class)
public class ScanRequest<T extends BleDevice> {

    private static final String TAG = "ScanRequest";
    private static final String HANDLER_TOKEN = "stop_token";
    private boolean scanning;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeScanner scanner;
    private ScanSettings scanSettings;
    private BleScanCallback<T> bleScanCallback;
    private List<ScanFilter> filters;
    private ArrayList<T> scanDevices = new ArrayList<>();
    private Handler handler = BleHandler.of();

    protected ScanRequest() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //scanner will be null if Bluetooth has been closed
            scanner = bluetoothAdapter.getBluetoothLeScanner();
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
            filters = new ArrayList<>();
        }

    }

    public void startScan(BleScanCallback<T> callback, long scanPeriod) {
        if (scanning) return;
        bleScanCallback = callback;
        scanning = true;
        // Stops scanning after a pre-defined scan period.
        HandlerCompat.postDelayed(handler, new Runnable() {
            @Override
            public void run() {
                if (scanning) {
                    stopScan();
                }
            }
        }, HANDLER_TOKEN, scanPeriod);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter.startLeScan(leScanCallback);
        } else {
            if (bluetoothAdapter.isEnabled()) {
                if (scanner == null) {
                    scanner = bluetoothAdapter.getBluetoothLeScanner();
                }
                /*byte[] manufacture = {0x00, 0x2A};
                filters.add(new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString("0000ae00-0000-1000-8000-00805f9b34fb"))
                        .setManufacturerData(0x5254, manufacture)
                        .build());*/
                setScanSettings();
                scanner.startScan(filters, scanSettings, scannerCallback);
            }
        }
        if (bleScanCallback != null) {
            bleScanCallback.onStart();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void setScanSettings() {
        boolean background = BleUtils.isBackground(Ble.getInstance().getContext());
        BleLog.i(TAG, "currently in the background:>>>>>"+background);
        if (background){
            UUID uuidService = Ble.options().getUuidService();
            filters.add(new ScanFilter.Builder()
                    .setServiceUuid(ParcelUuid.fromString(uuidService.toString()))//8.0以上手机后台扫描，必须开启
                    .build());
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
        }else {
            filters = new ArrayList<>();
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        }
    }

    public void stopScan() {
        if (!scanning) return;
        scanning = false;
        handler.removeCallbacksAndMessages(HANDLER_TOKEN);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            bluetoothAdapter.stopLeScan(leScanCallback);
        } else {
            if (bluetoothAdapter.isEnabled()) {
                if (scanner == null) {
                    scanner = bluetoothAdapter.getBluetoothLeScanner();
                }
                scanner.stopScan(scannerCallback);
            }
        }
        scanDevices.clear();
        if (bleScanCallback != null) {
            bleScanCallback.onStop();
            bleScanCallback = null;
        }
    }

    public boolean isScanning() {
        return scanning;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private ScanCallback scannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            T bleDevice = dispatcherScanResult(device, result.getRssi(), scanRecord);
            if (Ble.options().isParseScanData){
                ScanRecord parseRecord = ScanRecord.parseFromBytes(scanRecord);
                if (parseRecord != null && bleScanCallback != null) {
                    bleScanCallback.onParsedData(bleDevice, parseRecord);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                BleLog.i("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            BleLog.e("Scan Failed", "Error Code: " + errorCode);
            if (bleScanCallback != null){
                bleScanCallback.onScanFailed(errorCode);
            }
        }
    };

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            dispatcherScanResult(device, rssi, scanRecord);
        }
    };

    private T dispatcherScanResult(BluetoothDevice device, int rssi, byte[] scanRecord) {
        if (device == null) return null;
        T bleDevice = getDevice(device.getAddress());
        if (bleDevice == null) {
            bleDevice = BleFactory.create(device);
            if (bleScanCallback != null) {
                bleScanCallback.onLeScan(bleDevice, rssi, scanRecord);
            }
            scanDevices.add(bleDevice);
        } else {
            if (!Ble.options().isFilterScan) {//无需过滤
                if (bleScanCallback != null) {
                    bleScanCallback.onLeScan(bleDevice, rssi, scanRecord);
                }
            }
        }
        return bleDevice;
    }

    //获取已扫描到的设备（重复设备）
    private T getDevice(String address) {
        for (T device : scanDevices) {
            if (device.getBleAddress().equals(address)) {
                return device;
            }
        }
        return null;
    }

}
