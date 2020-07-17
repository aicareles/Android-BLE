package cn.com.heaton.blelibrary.ble.scan;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.os.Build;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ScanWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.Utils;

@TargetApi(Build.VERSION_CODES.LOLLIPOP)
class BluetoothScannerImplLollipop extends BleScannerCompat {
    private static final String TAG = "BluetoothScannerImplLol";

    private BluetoothLeScanner scanner;
    private ScanSettings scanSettings;
    private List<ScanFilter> filters = new ArrayList<>();

    @Override
    public void startScan(ScanWrapperCallback scanWrapperCallback) {
        super.startScan(scanWrapperCallback);
        if (scanner == null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        setScanSettings();
        scanner.startScan(filters, scanSettings, scannerCallback);
    }

    @Override
    public void stopScan() {
        if (scanner == null) {
            scanner = bluetoothAdapter.getBluetoothLeScanner();
        }
        scanner.stopScan(scannerCallback);
        super.stopScan();
    }

    private ScanCallback scannerCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            Log.e(TAG, "onScanResult: "+result.getScanRecord().getDeviceName());
            BluetoothDevice device = result.getDevice();
            byte[] scanRecord = result.getScanRecord().getBytes();
            if (scanWrapperCallback != null){
                scanWrapperCallback.onLeScan(device, result.getRssi(), scanRecord);
            }
            if (Ble.options().isParseScanData){
                ScanRecord parseRecord = ScanRecord.parseFromBytes(scanRecord);
                if (parseRecord != null && scanWrapperCallback != null) {
                    scanWrapperCallback.onParsedData(device, parseRecord);
                }
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
            for (ScanResult sr : results) {
                BleLog.d("ScanResult - Results", sr.toString());
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
            BleLog.e("Scan Failed", "Error Code: " + errorCode);
            if (scanWrapperCallback != null){
                scanWrapperCallback.onScanFailed(errorCode);
            }
        }
    };

    private void setScanSettings() {
        boolean background = Utils.isBackground(Ble.getInstance().getContext());
        BleLog.d(TAG, "currently in the background:>>>>>"+background);

        ScanFilter filter = Ble.options().getScanFilter();
        if (filter != null){
            filters.add(filter);
        }
        if (background){
            UUID uuidService = Ble.options().getUuidService();
            if (filter == null){
                filters.add(new ScanFilter.Builder()
                        .setServiceUuid(ParcelUuid.fromString(uuidService.toString()))//8.0以上手机后台扫描，必须开启
                        .build());
            }
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_POWER)
                    .build();
        }else {
            if (filter == null){
                filters.clear();
            }
            scanSettings = new ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build();
        }
    }
}
