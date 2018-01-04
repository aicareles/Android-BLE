package cn.com.heaton.blelibrary.ble.factory;

import android.util.Log;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import cn.com.heaton.blelibrary.ble.request.IRequest;

/**
 *
 * Created by LiuLei on 2017/12/28.
 */

public class RequestFactory<R extends IRequest> extends IRequestGenerator{

    private static volatile RequestFactory instance;
    private Map<String, IRequest> clazzMap = new HashMap<>();

    public static<R extends IRequest> RequestFactory<R> newInstance(){
        if (instance == null) {
            synchronized (RequestFactory.class) {
                if (instance == null) {
                    instance = new RequestFactory();
                }
            }
        }
        return instance;
    }

    @Override
    public <R extends IRequest> R generateRequest(Class<R> clazz){
        IRequest request = null;
        if(clazzMap.containsKey(clazz.getName())){
            request = clazzMap.get(clazz.getName());
            Log.e("RequestFactory", "generateRequest: 请求类对象"+clazz.getName()+"已经存在");
            return (R) request;
        }else {
            try {
                Constructor constructor = clazz.getDeclaredConstructor();
                constructor.setAccessible(true);
                try {
                    request = (IRequest) constructor.newInstance();
                    clazzMap.put(clazz.getName(), request);
                    return (R) request;
                } catch (InstantiationException e) {
                    e.printStackTrace();
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                } catch (InvocationTargetException e) {
                    e.printStackTrace();
                }
            } catch (NoSuchMethodException e) {
                e.printStackTrace();
            }
        }
        throw new ClassCastException("Class must implements IRequest");
    }
}
