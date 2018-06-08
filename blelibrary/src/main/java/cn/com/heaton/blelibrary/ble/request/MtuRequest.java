package cn.com.heaton.blelibrary.ble.request;

import android.os.Message;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(MtuRequest.class)
public class MtuRequest<T extends BleDevice> implements IMessage {

    private BleMtuCallback<T> mBleLisenter;

    protected MtuRequest() {
        BleHandler handler = BleHandler.getHandler();
        handler.setHandlerCallback(this);
    }

    public boolean setMtu(String address, int mtu, BleMtuCallback<T> lisenter){
        this.mBleLisenter = lisenter;
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (Ble.getInstance() != null && service != null) {
            result = service.setMTU(address, mtu);
        }
        return result;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case BleStates.BleStatus.MTUCHANGED:
                if(msg.obj instanceof BleDevice){
                    BleDevice device = (BleDevice) msg.obj;
                    mBleLisenter.onMtuChanged(device, msg.arg1, msg.arg2);
                }
                break;
            default:
                break;
        }
    }
}
