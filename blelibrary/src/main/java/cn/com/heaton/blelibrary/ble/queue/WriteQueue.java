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


    /**
     * 添加任务，
     * time 延迟时间,时间是毫秒
     * q 问题
     * 用户为问题设置延迟时间
     */
    /*@Override
    public void put(long time, RequestTask requestTask) {
        super.put(time, requestTask);
    }*/

    /*@Override
    public void remove(RequestTask requestTask) {
        super.remove(requestTask);
    }*/

    @Override
    public void execute(RequestTask requestTask) {
        bleRequest.wirteCharacteristic(requestTask.getAddress(), requestTask.getData());
    }

    /*@Override
    public void clear(){
        super.clear();
    }*/
}
