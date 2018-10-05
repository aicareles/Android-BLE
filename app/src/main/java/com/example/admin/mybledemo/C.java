package com.example.admin.mybledemo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * 创建时间:  2018/1/5
 * 创建人:Alex-Jerry
 * 功能描述: 全局静态常量值
 */

public class C {
    /**
     * SharePreferences常量保存类
     */
    public interface SP {
        String IS_FIRST_RUN = "isFirst";//程序是否是第一次运行
        String LANGUAGE = "language";//语言
        String CHINESE = "zh";//中文
        String ENGLISH = "en";//英文
    }

    //API静态
    @Retention(RetentionPolicy.SOURCE)
    public @interface API {
        String BASE_URL = "http://api.xxx.cn/api/";//服务器路径
        String APP_UPLOAD_INSTALL   = "app/count";//上传安装信息的接口
        String APP_LAST_UPDATE     = "app/lastUpdate";//获取应用新版本
    }

    //设置  id
    @Retention(RetentionPolicy.SOURCE)
    public @interface SET {
        int ID_LANGUAGE = 0;
        int ID_ABOUT = 1;
    }

    //语言  id
    @Retention(RetentionPolicy.SOURCE)
    public @interface LANGUAGE {
        int ID_ZN = 0;
        int ID_EN = 1;
        int ID_PT = 2;
        int ID_FR = 3;
        int ID_DE = 4;
    }

    //主页接收eventbus常量
    @Retention(RetentionPolicy.SOURCE)
    public @interface MAIN_EVENT {
        String SHUTDOWN = "shutdown";//断开设备时关机以及重置所有状态
        String ABNORMALLY_DISCONNECT = "abnormally_disconnect";//设备异常断开
        String CONNECTED = "connected";//设备重连成功
    }

    //全局静态常量
    @Retention(RetentionPolicy.SOURCE)
    public @interface Constance {
        String APP_HOST = "http://api.icworkshop.com/shop/";
        String OTA_OLD_PATH = "old_ota.bin";//需要更新老版本固件的本地路径
        String OTA_NEW_PATH = "new_ota.bin";//需要更新新版本固件的本地路径
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface Command {
        int BLE_CAR_COMMAND = 'C';
        int BLE_ORDER_COMMAND = 'O';
        int BLE_MUSIC_COMMAND = 'M';

        int TF_MUSIC_TYPE = 'T';
        int BT_MUSIC_TYPE = 'B';
        int GT_MUSIC_TYPE = 'G';
    }

}
