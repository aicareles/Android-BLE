package cn.com.heaton.blelibrary.ble.proxy;

import android.util.Log;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

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

    //绑定委托对象，并返回代理类
    public Object bindProxy(Object tar){
        this.tar = tar;
        //绑定委托对象，并返回代理类
        Log.e(TAG, "bindProxy: "+"绑定代理成功");
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
