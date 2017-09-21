package cn.com.heaton.blelibrary.ble;

import android.bluetooth.BluetoothDevice;
import android.content.Context;

/**
 * Created by LiuLei on 2017/5/3.
 * Bluetooth factory
 */

public class BleFactory<T extends BleDevice>{
    private Context mContext;

    public BleFactory(Context context){
        mContext = context;
    }

    public T create(BleManager<T> bleManager,BluetoothDevice device) throws Exception{
        return bleManager.getBleDevice(device);
    }

    public Context getContext() {
        return mContext;
    }
}
