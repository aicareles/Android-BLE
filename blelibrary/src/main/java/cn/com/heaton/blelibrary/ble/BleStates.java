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

public class BleStates {

    /**
     *  注释
     *  防止重复定义的常量值
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
            BleStatus.OnReady,
            BleStatus.ConnectFailed,
            BleStatus.ConnectError,
            BleStatus.ConnectException,
            BleStatus.ReadRssi,
            BleStatus.MTUCHANGED
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
        int ConnectFailed = 2521;
        int ConnectError = 2522;
        int ConnectException = 2523;
        int ReadRssi = 2524;
        int NotifySuccess = 2525;
        int MTUCHANGED = 2526;
    }

    @Retention(RetentionPolicy.SOURCE)
    public @interface StatusCode{
        int WriteFailed = -1;
        int WriteSuccess = 0;
        int WriteCancel = -2;
        int BleNotOpen = -3;
        int PermissionError = -4;
        int Processing = -5;
        int NotFindDevice = -6;
        int Data_Command_Error = -7;
    }

}
