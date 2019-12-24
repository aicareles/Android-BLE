package cn.com.heaton.blelibrary.ble.scan;

import android.bluetooth.BluetoothAdapter;
import android.os.Build;

import cn.com.heaton.blelibrary.ble.callback.wrapper.ScanWrapperCallback;

public abstract class BleScannerCompat {

    private static BleScannerCompat mInstance;
    BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    ScanWrapperCallback scanWrapperCallback;


    public static BleScannerCompat getScanner() {
        if (mInstance != null)
            return mInstance;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
            return mInstance = new BluetoothScannerImplLollipop();
        return mInstance = new BluetoothScannerImplJB();
    }

    public void startScan(ScanWrapperCallback scanWrapperCallback){
        this.scanWrapperCallback = scanWrapperCallback;
        scanWrapperCallback.onStart();
    }

    public void stopScan(){
        if (scanWrapperCallback != null){
            scanWrapperCallback.onStop();
        }
    }
}
