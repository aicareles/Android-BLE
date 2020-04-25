package cn.com.heaton.blelibrary.ble.queue;

import android.support.annotation.NonNull;

import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.request.Rproxy;

public final class ConnectQueue extends Queue{

    private static volatile ConnectQueue sInstance;
    protected ConnectRequest connectRequest;

    private ConnectQueue() {
        connectRequest = Rproxy.getRequest(ConnectRequest.class);
    }

    @NonNull
    public static ConnectQueue getInstance() {
        if (sInstance != null) {
            return sInstance;
        }
        synchronized (ConnectQueue.class) {
            if (sInstance == null) {
                sInstance = new ConnectQueue();
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
        connectRequest.reconnect(requestTask.getAddress());
    }

    /*@Override
    public void clear(){
        super.clear();
    }*/
}
