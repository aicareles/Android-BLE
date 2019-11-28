package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothDevice;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.wrapper.MtuWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.BleRequestImpl;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(MtuRequest.class)
public class MtuRequest<T extends BleDevice> implements MtuWrapperCallback {

    private BleMtuCallback<T> bleMtuCallback;

    protected MtuRequest() {}

    public boolean setMtu(String address, int mtu, BleMtuCallback<T> callback){
        this.bleMtuCallback = callback;
        boolean result = false;
        BleRequestImpl bleRequest = BleRequestImpl.getBleRequest();
        if (Ble.getInstance() != null && bleRequest != null) {
            result = bleRequest.setMtu(address, mtu);
        }
        return result;
    }

    @Override
    public void onMtuChanged(BluetoothDevice device, int mtu, int status) {
        if(null != bleMtuCallback){
            T bleDevice = Ble.<T>getInstance().getBleDevice(device);
            bleMtuCallback.onMtuChanged(bleDevice, mtu, status);
        }
    }
}
