package cn.com.heaton.blelibrary.ble;

import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.request.IRequest;

/**
 *
 * Created by LiuLei on 2017/10/30.
 */

public class BleHandler extends Handler {
    private static final String TAG = "BleHandler";
    private static BleHandler sHandler;//Handler for manipulating the Ble state

    private List<IRequest> receiveMessages = new ArrayList<>();

    public void setHandlerCallback(IRequest receiveMessage){
        if(!receiveMessages.contains(receiveMessage)){
            receiveMessages.add(receiveMessage);
        }
    }

    public static BleHandler getHandler(){
        synchronized (BleHandler.class){
            if(sHandler == null){
                HandlerThread handlerThread = new HandlerThread("handler thread");
                handlerThread.start();
                sHandler = new BleHandler(handlerThread.getLooper());
            }
            return sHandler;
        }
    }

    private BleHandler(Looper looper){
        super(Looper.myLooper());
    }

    @Override
    public void handleMessage(Message msg) {
        for(IRequest receiveMessage : receiveMessages){
            receiveMessage.handleMessage(msg);
        }
    }

}
