package cn.com.heaton.blelibrary;

import java.util.UUID;

/**
 * Created by admin on 2016/11/29.
 */

public class BleConfig {
    public final static int SCAN_PERIOD = 10000;//默认扫描时间

    public final static String UUID_DESCRIPTOR_TEXT     = "00002902-0000-1000-8000-00805f9b34fb";//描述UUID字符串
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public final static String UUID_SERVICE_TEXT        = "0000fee9-0000-1000-8000-00805f9b34fb";//服务UUID字符串
    public final static UUID UUID_SERVICE             = UUID.fromString(UUID_SERVICE_TEXT);//服务UUID字符串
    public final static String UUID_CHARACTERISTIC_TEXT = "d44bc439-abfd-45a2-b575-925416129600";//特性UUID字符串
    public final static             UUID   UUID_CHARACTERISTIC      = UUID.fromString(UUID_CHARACTERISTIC_TEXT);//特性UUID字符串

}
