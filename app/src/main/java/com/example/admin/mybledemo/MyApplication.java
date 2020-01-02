package com.example.admin.mybledemo;

import android.app.Application;
import android.content.Context;

import com.pgyersdk.Pgyer;
import com.pgyersdk.PgyerActivityManager;
import com.pgyersdk.crash.PgyCrashManager;
import com.pgyersdk.crash.PgyerCrashObservable;
import com.pgyersdk.crash.PgyerObserver;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.utils.BleUtils;
import cn.com.heaton.blelibrary.ble.utils.UuidUtils;
import cn.com.superLei.aoparms.AopArms;


/**
 * 应用入口
 * Created by LiuLei on 2016/4/25.
 * 　　　　　　　　┏┓　　　┏┓
 * 　　　　　　　┏┛┻━━━┛┻┓
 * 　　　　　　　┃　　　　　　　┃
 * 　　　　　　　┃　　　━　　　┃
 * 　　　　　　　┃　＞　　　＜　┃
 * 　　　　　　　┃　　　　　　　┃
 * 　　　　　　　┃...　⌒　...　┃
 * 　　　　　　　┃　　　　　　　┃
 * 　　　　　　　┗━┓　　　┏━┛
 * 　　　　　　　　　┃　　　┃　Code is far away from bug with the animal protecting
 * 　　　　　　　　　┃　　　┃   神兽保佑,代码无bug
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┃
 * 　　　　　　　　　┃　　　┗━━━┓
 * 　　　　　　　　　┃　　　　　　　┣┓
 * 　　　　　　　　　┃　　　　　　　┏┛
 * 　　　　　　　　　┗┓┓┏━┳┓┏┛
 * 　　　　　　　　　　┃┫┫　┃┫┫
 * 　　　　　　　　　　┗┻┛　┗┻┛
 */
public class MyApplication extends Application {

    private static MyApplication mApplication;

    @Override
    public void onCreate() {
        super.onCreate();
        mApplication = this;
        AopArms.init(this);
        initBle();
        initPgy();
    }

    //初始化蓝牙
    private void initBle() {
        Ble.options().setLogBleEnable(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setLogTAG("AndroidBLE")//设置全局蓝牙操作日志TAG
                .setAutoConnect(false)//设置是否自动连接
                .setFilterScan(false)//设置是否过滤扫描到的设备
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(12 * 1000)//设置扫描时长
                .setUuidService(UUID.fromString(UuidUtils.uuid16To128("fee9", true)))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))//设置可写特征的uuid
                .create(mApplication);
    }

    private void initPgy() {
        PgyCrashManager.register();
        PgyerCrashObservable.get().attach(new PgyerObserver() {
            @Override
            public void receivedCrash(Thread thread, Throwable throwable) {

            }
        });
        PgyerActivityManager.set(this);
    }

    @Override
    protected void attachBaseContext(Context context) {
        super.attachBaseContext(context);
        Pgyer.setAppId("b22d78f4563a1906c9c2605d9186c768");
    }

    public static MyApplication getInstance() {
        return mApplication;
    }

}
/**
 * 　                   &#######&
 * 　                   #########&
 * 　                  ###########&
 * 　                 ##&#$###$  ##&
 * 　                ;###  ####& ####
 * 　                ###;#######  ####
 * 　               &###########  #####
 * 　              ;#########o##   #####
 * 　              #########  ##   ######
 * 　            ;########### ###   ######
 * 　            ################   #######
 * 　           #################;   ######,
 * 　           #############$####   #######
 * 　          ########&;,########   &#######
 * 　        ;#########   &###       ########
 * 　        ##########    ###;      ########
 * 　       ###########     ##$      ########
 * 　       ###########     ###     #########
 * 　       ##########&$     ##    ;########
 * 　       #########, !      ##   ########
 * 　      ;########&          ##  #######
 * 　        #&#####            ##   &#&
 * 　          o# &#             #;
 * 　             ##             ##
 * 　             &#&           ;##
 * 　              ##           ###
 * <p/>
 * 　　　　　　　　　葱官赐福　　百无禁忌
 */
