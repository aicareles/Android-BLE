package cn.com.heaton.blelibrary.ble;

import android.support.annotation.IntDef;
import android.text.TextUtils;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.UUID;

/**
 * This class sets various static property values for Bluetooth
 * Created by liulei on 2016/11/29.
 */

public class BleConfig {

    public enum BleConstant{
        CONNECTED,
        CONNECTING,
        DISCONNECT,
        ConnectionChanged,
        ServicesDiscovered,
        Read,
        Write,
        Changed,
        DescriptorWriter,
        DescriptorRead,
        Start,
        Stop,
        ConnectTimeOut
    }

    /**
     *  Annotations
     *  prevent the defined constant values from being repeated
     */
    @IntDef({
            BleStatus.CONNECTED,
            BleStatus.CONNECTING,
            BleStatus.DISCONNECT,
            BleStatus.ConnectionChanged,
            BleStatus.ServicesDiscovered,
            BleStatus.Read,
            BleStatus.Write,
            BleStatus.Changed,
            BleStatus.DescriptorWriter,
            BleStatus.DescriptorRead,
            BleStatus.Start,
            BleStatus.Stop,
            BleStatus.ConnectTimeOut,
            BleStatus.OnReady
    })

    @Retention(RetentionPolicy.SOURCE)
    public @interface BleStatus{
        int CONNECTED = 2505;
        int CONNECTING = 2504;
        int DISCONNECT = 2503;
        int ConnectionChanged = 2511;
        int ServicesDiscovered = 2512;
        int Read = 2513;
        int Write = 2514;
        int Changed = 2515;
        int DescriptorWriter = 2516;
        int DescriptorRead = 2517;
        int Start = 2518;
        int Stop = 2519;
        int ConnectTimeOut = 2510;
        int OnReady = 2520;
    }
    /**
     *  The default scan time
     */
    public final static int SCAN_PERIOD = 10000;
    /**
     *   Connection time-out limit
     */
    public final static int CONNECT_TIME_OUT         = 10 * 1000;

    /**
     *    Broadcast corresponds to the product value
     */
    public static final byte[] BROADCAST_SPECIFIC_PRODUCT                       = {'T', 'R', 0, 1};

    /* Manufacturer Specific Data. */
    public static final int BLE_GAP_AD_TYPE_MANUFACTURER_SPECIFIC_DATA          = 0xFF;

    /**
     *  Describes the UUID string  00002901-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_DESCRIPTOR_TEXT     = "00002902-0000-1000-8000-00805f9b34fb";
    public static String CLIENT_CHARACTERISTIC_CONFIG = "00002902-0000-1000-8000-00805f9b34fb";
    /**
     *  The service UUID string  0000fee9-0000-1000-8000-00805f9b34fb
     */
    public static String UUID_SERVICE_TEXT        = "0000fee9-0000-1000-8000-00805f9b34fb";
    /**
     *   The service UUID string
     */
    public static UUID UUID_SERVICE             = UUID.fromString(UUID_SERVICE_TEXT);

    public static UUID UUID_DESCRIPTOR             = UUID.fromString(UUID_DESCRIPTOR_TEXT);
    /**
     *   Attribute UUID string d44bc439-abfd-45a2-b575-925416129600
     */
    public static String UUID_CHARACTERISTIC_TEXT = "d44bc439-abfd-45a2-b575-925416129600";
    /**
     *  Sets the notification attribute UUID string d44bc439-abfd-45a2-b575-925416129601
     */
    public static String UUID_NOTIFY_TEXT = "d44bc439-abfd-45a2-b575-925416129601";
    /**
     *  CHARACTERISTIC UUID string
     */
    public static             UUID   UUID_CHARACTERISTIC      = UUID.fromString(UUID_CHARACTERISTIC_TEXT);

    public static final String OtaWriteCharacteristic         = "013784cf-f7e3-55b4-6c4c-9fd140100a16";
    public static final String OtaNotifyCharacteristic        = "003784cf-f7e3-55b4-6c4c-9fd140100a16";

    public static final String QuinticOtaService              = "0000fee8-0000-1000-8000-00805f9b34fb";
    public static final UUID   UUID_OTA_WRITE_CHARACTERISTIC  = UUID.fromString(OtaWriteCharacteristic);
    public static final UUID   UUID_OTA_NOTIFY_CHARACTERISTIC = UUID.fromString(OtaNotifyCharacteristic);
    public static final UUID   UUID_QUINTIC_OTA_SERVICE       = UUID.fromString(QuinticOtaService);

    /**
     * Sets the service UUID string
     *
     * @param uuidServiceText Service UUID string
     */
    public static void setUuidServiceText(String uuidServiceText) {
        if (TextUtils.isEmpty(uuidServiceText)) {
            return;
        }
        UUID_SERVICE = UUID.fromString(uuidServiceText);
    }

    /**
     * Sets the Characteristic UUID string
     *
     * @param uuidCharacteristicText Characteristic UUID string
     */
    public static void setUuidCharacteristicText(String uuidCharacteristicText) {
        if (TextUtils.isEmpty(uuidCharacteristicText)) {
            return;
        }
        UUID_CHARACTERISTIC = UUID.fromString(uuidCharacteristicText);
    }

    /**
     * Sets the Description UUID string
     *
     * @param uuidDescriptorText Description UUID string
     */
    public static void setUuidDescriptorText(String uuidDescriptorText) {
        if (TextUtils.isEmpty(uuidDescriptorText)) {
            return;
        }
        UUID_DESCRIPTOR = UUID.fromString(uuidDescriptorText);
    }

    /**
     * Sets the Notification UUID string
     *
     * @param uuidNotifyText Notification UUID string
     */
    public static void setUuidNotifyText(String uuidNotifyText) {
        if (TextUtils.isEmpty(uuidNotifyText)) {
            return;
        }
        UUID_NOTIFY_TEXT = uuidNotifyText;
    }

    public static UUID getUuidService(){
        return UUID_SERVICE;
    }

    public static UUID getUuidCharacteristic(){
        return UUID_CHARACTERISTIC;
    }

    public static UUID getUuidDescriptor() {
        return UUID_DESCRIPTOR;
    }

    public static String getNotifyText(){
        return UUID_NOTIFY_TEXT;
    }

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

    public enum OtaCmd {
        OTA_CMD_META_DATA,
        OTA_CMD_BRICK_DATA,
        OTA_CMD_DATA_VERIFY,
        OTA_CMD_EXECUTION_NEW_CODE
    }

    public enum OtaResult {
        OTA_RESULT_SUCCESS,
        OTA_RESULT_PKT_CHECKSUM_ERROR,
        OTA_RESULT_PKT_LEN_ERROR,
        OTA_RESULT_DEVICE_NOT_SUPPORT_OTA,
        OTA_RESULT_FW_SIZE_ERROR,
        OTA_RESULT_FW_VERIFY_ERROR,
        OTA_RESULT_INVALID_ARGUMENT,
        OTA_RESULT_OPEN_FIRMWAREFILE_ERROR,
        OTA_RESULT_SEND_META_ERROR,
        OTA_RESULT_RECEIVED_INVALID_PACKET,
        OTA_RESULT_META_RESPONSE_TIMEOUT,
        OTA_RESULT_DATA_RESPONSE_TIMEOUT;
    }
}
