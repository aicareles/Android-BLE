package cn.com.heaton.blelibrary.ble.request;
import java.util.ArrayList;
import java.util.List;
import cn.com.heaton.blelibrary.ble.BleHandler;

public class BleConnectTask<T> implements Runnable {

    interface NextCallback<T> {
        void onNext(T device);
    }

    private static final long DEFALUT_DELAY = 2000L;
    private List<T> connectDevices = new ArrayList<>();
    private NextCallback<T> callback;

    void excute(List<T> devices, NextCallback<T> callback){
        this.callback = callback;
        connectDevices.addAll(devices);
        BleHandler.of().post(this);
    }

    /**
     * 获取暂未连接的设备数
     */
    int getLastSize(){
        return connectDevices.size();
    }

    //队列中是否包含该设备
    boolean isContains(T device){
        return connectDevices.contains(device);
    }

    void cancelAll(){
        connectDevices.clear();
    }

    void cancelOne(T device){
        connectDevices.remove(device);
    }

    @Override
    public void run() {
        if (null != connectDevices && !connectDevices.isEmpty()){
            if (null != callback){
                callback.onNext(connectDevices.get(0));
                connectDevices.remove(0);
                if (connectDevices.isEmpty())return;
                BleHandler.of().postDelayed(BleConnectTask.this, DEFALUT_DELAY);
            }
        }else {
            callback = null;
        }
    }
}
