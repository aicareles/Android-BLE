package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */
@Implement(NotifyRequest.class)
public class NotifyRequest<T extends BleDevice> implements IMessage {

    private static final String TAG = "NotifyRequest";

//    private BleNotiftCallback<T> mBleLisenter;

    private List<BleNotiftCallback> mNotifyCallbacks = new ArrayList<>();

    protected NotifyRequest() {
        BleHandler handler = BleHandler.getHandler();
        handler.setHandlerCallback(this);
        L.e(TAG, "NotifyRequest: ++++");
    }

    public void notify(T device, BleNotiftCallback<T> callback){
        this.mNotifyCallbacks.add(callback);
    }

    @Override
    public void handleMessage(Message msg) {
        if(msg.obj == null)return;
        switch (msg.what){
            case BleStates.BleStatus.ServicesDiscovered:
                for(BleNotiftCallback callback : mNotifyCallbacks){
                    callback.onServicesDiscovered((BluetoothGatt) msg.obj);
                }
                break;
            case BleStates.BleStatus.NotifySuccess:
                for (BleNotiftCallback callback : mNotifyCallbacks){
                    callback.onNotifySuccess((BluetoothGatt) msg.obj);
                }
                break;
            case BleStates.BleStatus.Changed:
                for(BleNotiftCallback callback : mNotifyCallbacks){
                    callback.onChanged((BluetoothGattCharacteristic) msg.obj);
                }
                break;
            default:
                break;
        }
    }
}
