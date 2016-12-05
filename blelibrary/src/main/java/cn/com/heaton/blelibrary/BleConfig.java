package cn.com.heaton.blelibrary;

import java.util.UUID;

/**
 * Created by liulei on 2016/11/29.
 */

public class BleConfig {
    //The default scan time
    public final static int SCAN_PERIOD = 10000;
    // Connection time-out limit
    public final static int CONNECT_TIME_OUT         = 10 * 1000;

    //Connection successful status
    public final static int CONNECTED = 2505;
    //The connection is in progress
    public final static int CONNECTING = 2504;
    //The disconnected state is a disconnected state
    public final static int DISCONNECT = 2503;
    //Broadcast corresponds to the product value
    public static final byte[] BROADCAST_SPECIFIC_PRODUCT                       = {'T', 'R', 0, 1};

    /* Manufacturer Specific Data. */
    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA          = 0xFF;

    //Describes the UUID string  00002901-0000-1000-8000-00805f9b34fb
    public final static String UUID_DESCRIPTOR_TEXT     = "00002902-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    //The service UUID string  0000fee9-0000-1000-8000-00805f9b34fb
    public final static String UUID_SERVICE_TEXT        = "0000fee9-0000-1000-8000-00805f9b34fb";
    //The service UUID string
    public final static UUID UUID_SERVICE             = UUID.fromString(UUID_SERVICE_TEXT);
    //Attribute UUID string d44bc439-abfd-45a2-b575-925416129600
    public final static String UUID_CHARACTERISTIC_TEXT = "d44bc439-abfd-45a2-b575-925416129600";
    //Sets the notification attribute UUID string d44bc439-abfd-45a2-b575-925416129601
    public final static String UUID_NOTIFY_TEXT = "d44bc439-abfd-45a2-b575-925416129601";
    //CHARACTERISTIC UUID string
    public final static             UUID   UUID_CHARACTERISTIC      = UUID.fromString(UUID_CHARACTERISTIC_TEXT);

    /**
     * Verify the product broadcast parameters
     * @param data Parameter data
     * @return Whether the match
     */
    public static boolean matchProduct(byte[] data) {
        if (data == null || data.length <= 0) {
            return false;
        }
        int i = 0;
        do {
            // Read packet size
            int len = data[i++] & 0xff;
            if (len > 0) {
                // Read packet data
                byte[] d = new byte[len];
                int j = 0;
                do {
                    d[j++] = data[i++];
                } while (j < len);
                // Authentication Type and Length
                if (d.length > BROADCAST_SPECIFIC_PRODUCT.length && (d[0] & 0xFF) == BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA) {
                    // Matching product parameters
                    boolean passed = true;
                    for (int x = 0; x < BROADCAST_SPECIFIC_PRODUCT.length; x++) {
                        passed = passed && d[x + 1] == BROADCAST_SPECIFIC_PRODUCT[x];
                    }
                    //Match successful
                    if (passed) {
                        return true;
                    }
                }
            }

        } while (i < data.length);
        return false;
    }
}
