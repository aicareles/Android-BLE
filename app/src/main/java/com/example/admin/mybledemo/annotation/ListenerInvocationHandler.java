package com.example.admin.mybledemo.annotation;

import android.app.Activity;
import android.content.Context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.Map;

/**
 * Created by jerry on 2018/6/13.
 */

public class ListenerInvocationHandler  implements InvocationHandler {
    //activity   真实对象
    private Activity activity;
    private Map<String,Method> methodMap;

    public ListenerInvocationHandler(Activity activity, Map<String, Method> methodMap) {
        this.activity = activity;
        this.methodMap = methodMap;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String name=method.getName();
        //决定是否需要进行代理
        Method metf=methodMap.get(name);

        if(metf!=null) {
            return  metf.invoke(activity,args);
        }else {
            return method.invoke(proxy,args);
        }
    }
}
