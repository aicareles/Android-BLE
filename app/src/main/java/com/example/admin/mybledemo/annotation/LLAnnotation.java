package com.example.admin.mybledemo.annotation;

import android.app.Activity;
import android.view.View;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 自定义注解   替代findViewById
 * Created by LiuLei on 2017/6/9.
 */

public class LLAnnotation {

    public static void viewInit(final Activity activity){
        Class<?> cls = activity.getClass();

        // 遍历属性
        Field[] fields = cls.getDeclaredFields();
        if(fields != null && fields.length > 0){
            for (Field field : fields){
                //获取字段的注解，如果没有ViewInit注解，则返回null
                ViewInit viewInit = field.getAnnotation(ViewInit.class);
                if(viewInit != null){
                    //获取字段注解的参数，这就是我们传进去控件ID
                    int viewId = viewInit.value();
                    if(viewId != -1){
                        try {
                            // 获取类中的findViewById方法，参数为int
                            Method method = cls.getMethod("findViewById",int.class);
                            //执行该方法，返回一个Object类型的View实例
                            Object resView = method.invoke(activity,viewId);
                            field.setAccessible(true);
                            //把字段的值设置为该View的实例
                            field.set(activity,resView);
                        } catch (NoSuchMethodException e) {
                            e.printStackTrace();
                        }catch (IllegalAccessException e){
                            e.printStackTrace();
                        }catch (InvocationTargetException e){
                            e.printStackTrace();
                        }
                    }
                }
            }
        }

        //遍历方法
        Method[] methods = cls.getDeclaredMethods();
        for(final Method method : methods){
            //找到有OnClick注解的方法
            OnClick onClick = method.getAnnotation(OnClick.class);
            if(onClick != null){
                //通过id获取到View，再对view设置点击事件
                activity.findViewById(onClick.value()).setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        try {
                            method.setAccessible(true);
                            //调用这个被OnClick注解的方法
                            method.invoke(activity);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        } catch (InvocationTargetException e) {
                            e.printStackTrace();
                        }
                    }
                });
            }
        }
    }

}
