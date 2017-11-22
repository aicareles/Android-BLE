package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.os.Message;
import android.util.Log;

import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.BleStates;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;

/**
 *
 * Created by LiuLei on 2017/10/23.
 */

public class NotifyRequest<T> implements BleHandler.ReceiveMessage {

    private static final String TAG = "NotifyRequest";

    private BleNotiftCallback<BleDevice> mBleLisenter;
    private static volatile NotifyRequest instance;

    public static NotifyRequest getInstance(){
        if (instance == null) {
            synchronized (NotifyRequest.class) {
                if (instance == null) {
                    instance = new NotifyRequest();
                }
            }
        }
        return instance;
    }

    private NotifyRequest() {
        BleHandler handler = BleHandler.getHandler();
        handler.setHandlerCallback(this);
        BleLog.e(TAG, "NotifyRequest: ++++");
    }

    public void notify(BleDevice device, BleNotiftCallback<BleDevice> callback){
        this.mBleLisenter = callback;
    }

    @Override
    public void handleMessage(Message msg) {
        if(msg.obj == null)return;
        switch (msg.what){
            case BleStates.BleStatus.ServicesDiscovered:
                mBleLisenter.onServicesDiscovered((BluetoothGatt) msg.obj);
                break;
            case BleStates.BleStatus.NotifySuccess:
                mBleLisenter.onNotifySuccess((BluetoothGatt) msg.obj);
                break;
            case BleStates.BleStatus.Changed:
                mBleLisenter.onChanged((BluetoothGattCharacteristic) msg.obj);
                break;
            default:
                break;
        }
    }
}
