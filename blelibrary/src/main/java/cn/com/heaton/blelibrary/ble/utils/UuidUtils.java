package cn.com.heaton.blelibrary.ble.utils;

import java.util.UUID;

public class UuidUtils {
    private static final String base_uuid_regex = "0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb";

    public static boolean isBaseUUID(String uuid) {
        return uuid.toLowerCase().matches("0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb");
    }

    public static String uuid128To16(String uuid, boolean lower_case) {
        String uuid_16 = "";
        if (uuid.length() == 36) {
            if (lower_case) {
                uuid_16 = uuid.substring(4, 8).toLowerCase();
            }else {
                uuid_16 = uuid.substring(4, 8).toUpperCase();
            }
            return uuid_16;
        }
        return null;
    }

    public static String uuid16To128(String uuid, boolean lower_case) {
        String uuid_128 = "";
        if (lower_case) {
            uuid_128 = ("0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb".substring(0, 4) + uuid + "0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb".substring(38)).toLowerCase();
        }else {
            uuid_128 = ("0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb".substring(0, 4) + uuid + "0000([0-9a-f][0-9a-f][0-9a-f][0-9a-f])-0000-1000-8000-00805f9b34fb".substring(38)).toUpperCase();
        }
        return uuid_128;
    }
}
