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
    public void onStart(){};

    /**
     *  Stop scanning
     */
    public void onStop(){};

    /**
     * Scan to device
     * @param device ble device object
     * @param rssi rssi
     * @param scanRecord Bluetooth radio package
     */
    public abstract void onLeScan(T device, int rssi, byte[] scanRecord);

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public void onParsedData(T device, ScanRecord scanRecord){};
}
