package cn.com.heaton.blelibrary.ble.request;

import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.wrapper.ReadRssiWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(ReadRssiRequest.class)
public class ReadRssiRequest<T extends BleDevice> implements ReadRssiWrapperCallback<T> {

    private BleReadRssiCallback<T> readRssiCallback;
    private final BleRequestImpl<T> bleRequest = BleRequestImpl.getBleRequest();

    public boolean readRssi(T device, BleReadRssiCallback<T> callback){
        this.readRssiCallback = callback;
        boolean result = false;
        if (bleRequest != null) {
            result = bleRequest.readRssi(device.getBleAddress());
        }
        return result;
    }

    @Override
    public void onReadRssiSuccess(T device, int rssi) {
        if(readRssiCallback != null){
            readRssiCallback.onReadRssiSuccess(device, rssi);
        }
    }
}
