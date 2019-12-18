package cn.com.heaton.blelibrary.ble.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.List;

/**
 * description $desc$
 * created by jerry on 2019/8/14.
 */
public class BleUtils {

    public static final String PROPERTY_SIGNED_WRITE = "authenticatedSignedWrites";
    public static final String PROPERTY_BROADCAST = "broadcast";
    public static final String PROPERTY_EXTENDED_PROPS = "extendedProperties";
    public static final String PROPERTY_INDICATE = "indicate";
    public static final String PROPERTY_NOTIFY = "notify";
    public static final String PROPERTY_READ = "read";
    public static final String PROPERTY_WRITE = "write";
    public static final String PROPERTY_WRITE_NO_RESPONSE = "writeWithoutResponse";

    private static HashMap<Integer, String> propertys = new HashMap<Integer, String>();
    static {
        propertys.put(1, PROPERTY_BROADCAST);
        propertys.put(2, PROPERTY_READ);
        propertys.put(4, PROPERTY_WRITE_NO_RESPONSE);
        propertys.put(8, PROPERTY_WRITE);
        propertys.put(16, PROPERTY_NOTIFY);
        propertys.put(32, PROPERTY_INDICATE);
        propertys.put(64, PROPERTY_SIGNED_WRITE);
        propertys.put(128, PROPERTY_EXTENDED_PROPS);
    }


    public static boolean isBackground(Context context) {
        ActivityManager activityManager = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> appProcesses = activityManager
                .getRunningAppProcesses();
        for (ActivityManager.RunningAppProcessInfo appProcess : appProcesses) {
            if (appProcess.processName.equals(context.getPackageName())) {
                if (appProcess.importance != ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public static String lookup(int propertySum, int property) {
        if ((propertySum & property) == property) {
            String propertyName = propertys.get(property);
            return propertyName == null ? null : propertyName;
        } else {
            return null;
        }
    }

    public static boolean isGpsOpen(Context context) {
        int locationMode = 0;
        String locationProviders;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            try {
                locationMode = Settings.Secure.getInt(context.getContentResolver(), Settings.Secure.LOCATION_MODE);
            } catch (Settings.SettingNotFoundException e) {
                e.printStackTrace();
                return false;
            }
            return locationMode != Settings.Secure.LOCATION_MODE_OFF;
        } else {
            locationProviders = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
            return !TextUtils.isEmpty(locationProviders);
        }
    }



}
