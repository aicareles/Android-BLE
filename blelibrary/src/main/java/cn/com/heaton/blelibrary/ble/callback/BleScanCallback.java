package cn.com.heaton.blelibrary.ble.callback;

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
}
