package com.example.admin.mybledemo.annotation;

import android.app.Activity;
import android.util.Log;
import android.view.View;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

/**
 * 自定义注解   替代findViewById
 * Created by LiuLei on 2017/6/9.
 */

public class LLAnnotation {

    public static void bind(Activity activity){
        //注入布局
        bindLayout(activity);
        //注入视图
        bindViews(activity);
        //注入事件
        bindEvents(activity);
    }

    private static void bindEvents(Activity activity) {
//        Class<?> clazz=activity.getClass();
//        //获取Activity里面 所有方法
//        Method[] methods=clazz.getDeclaredMethods();
//        //遍历Activity所有方法
//        for (Method method:methods)
//        {
//            //获取方法上所有的注解
//            Annotation[]  annotations=method.getAnnotations();
//            for(Annotation annotation:annotations)
//            {
//                //获取注解 anntionType   OnClick  OnLongClck
//                Class<?> anntionType=annotation.annotationType();
//                //获取注解的注解   onClick 注解上面的EventBase
//                EventInterface eventBase=anntionType.getAnnotation(EventInterface.class);
//                if(eventBase==null) {
//                    continue;
//                }
//                /*
//                开始获取事件三要素  通过反射注入进去
//                1 listenerSetter  返回     setOnClickListener字符串
//                 */
//                String listenerSetter=eventBase.lisenterSetter();
//                //得到 listenerType--》 View.OnClickListener.class,
//                Class<?> listenerType=eventBase.lisenterType();
//                //callMethod--->onClick
//                String callMethod=eventBase.callbackMethod();
//
//                Log.e("bindEvents", "bindEvents: "+listenerSetter+"listenerType:"+listenerType+"callMethod:"+callMethod);
//                //方法名 与方法Method的对应关心
//                Map<String,Method> methodMap=new HashMap<>();
//
//                methodMap.put(callMethod,method);
//
//                try {
//                    Method valueMethod=anntionType.getDeclaredMethod("value");
//                    int[] viewIds= (int[]) valueMethod.invoke(annotation);
//                    for (int viewId:viewIds)
//                    {
//                        //通过反射拿到TextView
//                        Method findViewById=clazz.getMethod("findViewById",int.class);
//                        View view= (View) findViewById.invoke(activity,viewId);
//                        if(view==null) {
//                            continue;
//                        }
//                        /*
//                        listenerSetter  setOnClickLitener
//                        listenerType   View.OnClickListener.class
//                         */
//                        Method setOnClickListener=view.getClass().getMethod(listenerSetter,listenerType);
//
//                        ListenerInvocationHandler handler=new ListenerInvocationHandler(activity,methodMap);
////                        proxyy已经实现了listenerType接口
//                        Object proxy= Proxy.newProxyInstance
//                                (listenerType.getClassLoader(),
//                                        new Class[]{listenerType},handler);
//                        /**
//                         * 类比 于  textView.setOnClickListener(new View.OnClickListener() {
//                         @Override
//                         public void onClick(View v) {
//                         }
//                         });
//                         */
//                        setOnClickListener.invoke(view,proxy);
//                    }
//                } catch (NoSuchMethodException e) {
//                    e.printStackTrace();
//                } catch (InvocationTargetException e) {
//                    e.printStackTrace();
//                } catch (IllegalAccessException e) {
//                    e.printStackTrace();
//                }
//
//            }
//
//        }
        // 获取方法上面的注解
        Class myClass = activity.getClass();
        Method myMethod[] = myClass.getDeclaredMethods();// 先拿到全部方法
        for (Method method : myMethod) {
            Annotation[] annotations = method.getAnnotations();
            for (Annotation annotation : annotations) {
                Class<?> annotationType=annotation.annotationType();
                EventInterface eventInterface = annotationType.getAnnotation(EventInterface.class);// 拿到注解里面的注解
                if(eventInterface==null) {
                    continue;
                }
                // 得到事件的三要素
                String listenerSetter = eventInterface.lisenterSetter();
                Class listenerType = eventInterface.lisenterType();
                String callbackMethod = eventInterface.callbackMethod();
                // 获取注解事件的控件对象Button
                try {
                    Method valueMethod = annotationType.getDeclaredMethod("value");
                    try {
                        int[] viewIds = (int[])valueMethod.invoke(annotation);
                        for (int viewId : viewIds) {
                            View view = activity.findViewById(viewId);
                            // 反射setOnClickListener方法,这里要用到代理
                            Method setListenerMethod = view.getClass().getMethod(listenerSetter, listenerType);
                            Map methodMap = new HashMap();
                            methodMap.put(callbackMethod, method);
                            InvocationHandler invocationHandler = new ListenerInvocationHandler(activity, methodMap);
                            Object newProxyInstance = Proxy.newProxyInstance(listenerType.getClassLoader(), new Class[]{listenerType}, invocationHandler);
                            setListenerMethod.invoke(view , newProxyInstance);
                        }
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (IllegalArgumentException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    }
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void bindViews(Activity activity) {
        // 获取每一个属性上的注解
        Class myClass = activity.getClass();
        Field[] myFields = myClass.getDeclaredFields();// 先拿到里面所有的成员变量
        for (Field field : myFields) {
            ViewInit myView = field.getAnnotation(ViewInit.class);
            if (myView != null) {
                int value = myView.value();// 拿到属性id
                View view = activity.findViewById(value);
                // 将view赋值给类里面的属性
                try {
                    field.setAccessible(true);// 为了防止其实私有的的，需要设置允许访问
                    field.set(activity,view);
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (IllegalArgumentException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void bindLayout(Activity activity) {
        // 获取我们自定义类CastielContentViewInject上面的注解
        Class myClass = activity.getClass();
        ContentView contentView = (ContentView) myClass.getAnnotation(ContentView.class);
        if(contentView != null){
            int myLayoutResId = contentView.value();
            activity.setContentView(myLayoutResId);
        }
    }

}
