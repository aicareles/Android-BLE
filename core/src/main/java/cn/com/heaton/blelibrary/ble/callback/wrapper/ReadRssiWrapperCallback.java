package cn.com.heaton.blelibrary.ble.callback.wrapper;

/**
 * Created by LiuLei on 2017/10/23.
 */

public interface ReadRssiWrapperCallback<T> {

    void onReadRssiSuccess(T device, int rssi);
}
