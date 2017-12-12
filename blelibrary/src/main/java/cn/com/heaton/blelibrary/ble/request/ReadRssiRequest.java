package cn.com.heaton.blelibrary.ble.request;

import android.os.Message;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.BluetoothLeService;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public class ReadRssiRequest<T extends BleDevice> implements BleHandler.ReceiveMessage {

    private BleReadRssiCallback<T> mBleLisenter;

    private static volatile ReadRssiRequest instance;
    public static <T extends BleDevice> ReadRssiRequest<T> getInstance(){
        if (instance == null) {
            synchronized (ReadRequest.class) {
                if (instance == null) {
                    instance = new ReadRssiRequest();
                }
            }
        }
        return instance;
    }

    private ReadRssiRequest() {
        BleHandler handler = BleHandler.getHandler();
        handler.setHandlerCallback(this);
    }

    public boolean readRssi(T device, BleReadRssiCallback<T> lisenter){
        this.mBleLisenter = lisenter;
        boolean result = false;
        BluetoothLeService service = Ble.getInstance().getBleService();
        if (Ble.getInstance() != null && service != null) {
            result = service.readRssi(device.getBleAddress());
        }
        return result;
    }

    @Override
    public void handleMessage(Message msg) {
        switch (msg.what){
            case BleStates.BleStatus.ReadRssi:
                if(msg.obj instanceof Integer){
                    int rssi = (int) msg.obj;
                    mBleLisenter.onReadRssiSuccess(rssi);
                }
                break;
            default:
                break;
        }
    }
}
