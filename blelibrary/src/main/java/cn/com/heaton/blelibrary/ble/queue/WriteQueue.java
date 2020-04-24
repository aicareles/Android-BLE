package cn.com.heaton.blelibrary.ble.queue;

import android.support.annotation.NonNull;

import cn.com.heaton.blelibrary.ble.BleRequestImpl;

public final class WriteQueue extends Queue{

    private static volatile WriteQueue sInstance;
    protected BleRequestImpl bleRequest;

    private WriteQueue() {
        bleRequest = BleRequestImpl.getBleRequest();
    }

    @NonNull
    public static WriteQueue getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (WriteQueue.class) {
            if (sInstance == null) {
                sInstance = new WriteQueue();
            }
        }
        return sInstance;
    }

    @Override
    public void execute(RequestTask requestTask) {
        bleRequest.wirteCharacteristic(requestTask.getAddress(), requestTask.getData());
    }

}
