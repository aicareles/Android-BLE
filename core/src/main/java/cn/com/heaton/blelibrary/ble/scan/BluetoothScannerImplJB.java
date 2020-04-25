package cn.com.heaton.blelibrary.ble.scan;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ScanWrapperCallback;

class BluetoothScannerImplJB extends BleScannerCompat {

    @Override
    public void startScan(ScanWrapperCallback scanWrapperCallback) {
        super.startScan(scanWrapperCallback);
        bluetoothAdapter.startLeScan(leScanCallback);
    }

    @Override
    public void stopScan() {
        super.stopScan();
        bluetoothAdapter.stopLeScan(leScanCallback);
    }

    private BluetoothAdapter.LeScanCallback leScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, int rssi, final byte[] scanRecord) {
            scanWrapperCallback.onLeScan(device, rssi, scanRecord);
        }
    };
}
