package cn.com.heaton.blelibrary.ble.callback;

import android.os.Build;
import android.support.annotation.RequiresApi;

import cn.com.heaton.blelibrary.ble.model.ScanRecord;

/**
 * Created by LiuLei on 2017/10/23.
 */

public abstract class BleScanCallback<T> {
    /**
     *  Start the scan
     */
    public void onStart(){}

    /**
     *  Stop scanning
     */
    public void onStop(){}

    /**
     * Scan to device
     * @param device ble device object
     * @param rssi rssi
     * @param scanRecord Bluetooth radio package
     */
    public abstract void onLeScan(T device, int rssi, byte[] scanRecord);

    /**
     * errorCode=1;Fails to start scan as BLE scan with the same settings is already started by the app.
     * errorCode=2;Fails to start scan as app cannot be registered.
     * errorCode=3;Fails to start scan due an internal error
     * errorCode=4;Fails to start power optimized scan as this feature is not supported
     * @param errorCode 扫描错误码
     */
    public void onScanFailed(int errorCode){}

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onParsedData(T device, ScanRecord scanRecord){}
}
