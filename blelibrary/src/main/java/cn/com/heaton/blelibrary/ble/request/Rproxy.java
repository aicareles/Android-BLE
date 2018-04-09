package cn.com.heaton.blelibrary.ble.request;

import java.lang.annotation.Annotation;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import cn.com.heaton.blelibrary.ble.annotation.Implement;

/**
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
        List<Class>list = new LinkedList<>();
        for(Class cls : clss){
            if(cls.isAnnotationPresent(Implement.class)){
                list.add(cls);
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


}
