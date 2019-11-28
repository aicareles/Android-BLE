package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;

import cn.com.heaton.blelibrary.ble.Ble;
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
public class ReadRssiRequest<T extends BleDevice> implements ReadRssiWrapperCallback {

    private BleReadRssiCallback<T> readRssiCallback;
    private Ble<T> ble = Ble.getInstance();

    protected ReadRssiRequest() {
    }

    public boolean readRssi(T device, BleReadRssiCallback<T> callback){
        this.readRssiCallback = callback;
        boolean result = false;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (Ble.getInstance() != null && bleRequest != null) {
            result = bleRequest.readRssi(device.getBleAddress());
        }
        return result;
    }

    @Override
    public void onReadRssiSuccess(BluetoothDevice device, int rssi) {
        if(readRssiCallback != null){
            readRssiCallback.onReadRssiSuccess(ble.getBleDevice(device), rssi);
        }
    }
}
