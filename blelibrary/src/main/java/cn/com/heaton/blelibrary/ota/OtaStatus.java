package cn.com.heaton.blelibrary.ota;

/**
 * Created by LiuLei on 2017/10/19.
 */

public class OtaStatus {

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
