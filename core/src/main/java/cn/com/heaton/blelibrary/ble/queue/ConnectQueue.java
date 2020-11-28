package cn.com.heaton.blelibrary.ble.queue;

import android.support.annotation.NonNull;
import android.util.Log;

import cn.com.heaton.blelibrary.ble.BleLog;
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

    @Override
    public void execute(RequestTask requestTask) {
        connectRequest.reconnect(requestTask.getAddress());
//        BleLog.i("ConnectQueue", "正在重新连接设备:>>>"+"result:"+reconnect+">>>>"+requestTask.getAddress());
    }

}
