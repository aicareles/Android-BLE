package cn.com.heaton.blelibrary.ble.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.request.ConnectRequest;
import cn.com.heaton.blelibrary.ble.request.NotifyRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRequest;
import cn.com.heaton.blelibrary.ble.request.ReadRssiRequest;
import cn.com.heaton.blelibrary.ble.request.Rproxy;
import cn.com.heaton.blelibrary.ble.request.ScanRequest;
import cn.com.heaton.blelibrary.ble.request.WriteRequest;

/**
 *
 * Created by LiuLei on 2017/9/1.
 */

public class RequestProxy implements InvocationHandler{
    private static final String TAG = "RequestProxy";

    private Object tar;

    private static RequestProxy instance = new RequestProxy();


    public static RequestProxy getInstance(){
        return instance;
    }

    //Bind the delegate object and return the proxy class
    public Object bindProxy(Object tar){
        this.tar = tar;
        //绑定委托对象，并返回代理类
        L.e(TAG, "bindProxy: "+"Binding agent successfully");
        Rproxy.getInstance().init(ScanRequest.class,
                ConnectRequest.class,
                NotifyRequest.class,
                ReadRequest.class,
                ReadRssiRequest.class,
                WriteRequest.class
        );
        return Proxy.newProxyInstance(
                tar.getClass().getClassLoader(),
                tar.getClass().getInterfaces(),
                this);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        return method.invoke(tar,args);
    }
}
