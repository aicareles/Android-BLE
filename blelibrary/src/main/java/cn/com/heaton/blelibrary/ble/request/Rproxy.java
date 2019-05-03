package cn.com.heaton.blelibrary.ble.request;

import android.content.Context;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.com.heaton.blelibrary.ble.annotation.Implement;
import dalvik.system.DexFile;

/**
 *
 * Created by LiuLei on 2018/1/22.
 */

public class Rproxy {
    private static final Rproxy s_instance = new Rproxy();

    private Map<Class, Object> mRequestObjs;

    public static Rproxy getInstance(){
        return s_instance;
    }

    private Rproxy(){
        mRequestObjs = new HashMap<>();
    }

    public void init(Class... clss){
//        List<Class> requestsClass = getRequestsClass(context, getClass().getPackage().getName());
        for(Class cls : clss){
            if(cls.isAnnotationPresent(Implement.class)){
                for(Annotation ann : cls.getDeclaredAnnotations()){
                    if(ann instanceof Implement){
                        try {
                            mRequestObjs.put(cls, ((Implement) ann).value().newInstance());
                        } catch (InstantiationException e) {
                            e.printStackTrace();
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }
        }
    }

    public <T>T getRequest(Class cls){
        return (T) mRequestObjs.get(cls);
    }

    private List<Class> getRequestsClass(Context context, String packageName){
        List<Class> classRequestsList = new ArrayList<>();
        try {
            DexFile df = new DexFile(context.getPackageCodePath());//通过DexFile查找当前的APK中可执行文件
            Enumeration<String> enumeration = df.entries();//获取df中的元素  这里包含了所有可执行的类名 该类名包含了包名+类名的方式
            while (enumeration.hasMoreElements()) {//遍历
                String className = (String) enumeration.nextElement();
                if (className.contains(packageName) && !className.contains("$")) {//在当前所有可执行的类里面查找包含有该包名的所有类
                    try {
                        Class requestCls = Class.forName(className);
                        classRequestsList.add(requestCls);
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return  classRequestsList;
    }


}
