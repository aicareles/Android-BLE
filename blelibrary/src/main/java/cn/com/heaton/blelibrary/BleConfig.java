package cn.com.heaton.blelibrary;

import java.util.UUID;

/**
 * Created by liulei on 2016/11/29.
 */

public class BleConfig {
    public final static int SCAN_PERIOD = 10000;//默认扫描时间
    public final static int CONNECT_TIME_OUT         = 10 * 1000;// 连接超时时间限制

    public final static int CONNECTED = 2505;//连接成功状态
    public final static int CONNECTING = 2504;//正在连接状态
    public final static int DISCONNECT = 2503;//断开连接状态  为连接状态
    public static final byte[] BROADCAST_SPECIFIC_PRODUCT                       = {'T', 'R', 0, 1};//广播对应产品值

    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA          = 0xFF; /**< Manufacturer Specific Data. */

    public final static String UUID_DESCRIPTOR_TEXT     = "00002902-0000-1000-8000-00805f9b34fb";//描述UUID字符串
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    public final static String UUID_SERVICE_TEXT        = "0000fee9-0000-1000-8000-00805f9b34fb";//服务UUID字符串
    public final static UUID UUID_SERVICE             = UUID.fromString(UUID_SERVICE_TEXT);//服务UUID字符串
    public final static String UUID_CHARACTERISTIC_TEXT = "d44bc439-abfd-45a2-b575-925416129600";//特性UUID字符串
    public final static             UUID   UUID_CHARACTERISTIC      = UUID.fromString(UUID_CHARACTERISTIC_TEXT);//特性UUID字符串

    /**
     * 验证产品广播参数
     * @param data 参数数据
     * @return 是否匹配
     */
    public static boolean matchProduct(byte[] data) {
        if (data == null || data.length <= 0) {
            return false;
        }
        int i = 0;
        do {
            // 读取包大小
            int len = data[i++] & 0xff;
            if (len > 0) {
                // 读取包数据
                byte[] d = new byte[len];
                int j = 0;
                do {
                    d[j++] = data[i++];
                } while (j < len);
                // 验证类型 及长度
                if (d.length > BROADCAST_SPECIFIC_PRODUCT.length && (d[0] & 0xFF) == BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA) {
                    boolean passed = true;// 匹配产品参数
                    for (int x = 0; x < BROADCAST_SPECIFIC_PRODUCT.length; x++) {
                        passed = passed && d[x + 1] == BROADCAST_SPECIFIC_PRODUCT[x];
                    }
                    // 匹配成功
                    if (passed) {
                        return true;
                    }
                }
            }

        } while (i < data.length);
        return false;
    }
}
