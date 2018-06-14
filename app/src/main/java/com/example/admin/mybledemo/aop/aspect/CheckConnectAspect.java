package com.example.admin.mybledemo.aop.aspect;

import android.util.Log;
import android.view.View;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.aop.CheckConnect;
import com.example.admin.mybledemo.utils.ToastUtil;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

import java.util.Calendar;
import java.util.List;
import java.util.Objects;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.spp.BtDevice;
import cn.com.heaton.blelibrary.spp.BtManager;

/**
 * Created by jerry on 2018/6/13.
 */

@Aspect
public class CheckConnectAspect {

    private static final String CHECK_CONNECT = "execution(@com.example.admin.mybledemo.aop.CheckConnect * *(..))";

    @Pointcut("execution(@com.example.admin.mybledemo.aop.CheckConnect * *(..))")//方法切入点
    public void methodAnnotated() {
    }

//    @Around("methodAnnotated()")//在连接点进行方法替换

    @Around("execution(@com.example.admin.mybledemo.aop.CheckConnect * *(..)) && @annotation(checkConnect)")
    public void aroundJoinPoint(ProceedingJoinPoint joinPoint, CheckConnect checkConnect) throws Throwable {
        List list;
        if (checkConnect.value() == 1){
            list = BtManager.getBtManager().getConnectedDevices();
        }else {//其他任何值  默认是ble设备
            list = Ble.getInstance().getConnetedDevices();
        }
        if(list == null || list.size() == 0){
            ToastUtil.showToast("请先连接设备!");
            return;
        }
        joinPoint.proceed();//执行原方法
    }
}
