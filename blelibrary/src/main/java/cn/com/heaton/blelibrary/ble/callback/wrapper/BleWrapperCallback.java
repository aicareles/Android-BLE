package cn.com.heaton.blelibrary.ble.callback.wrapper;

import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;

public abstract class BleWrapperCallback<T extends BleDevice> extends BleScanCallback<T>
        implements ConnectWrapperCallback<T>, NotifyWrapperCallback<T>, WriteWrapperCallback<T>, ReadWrapperCallback<T>{}
