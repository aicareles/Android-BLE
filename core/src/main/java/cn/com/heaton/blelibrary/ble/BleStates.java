package cn.com.heaton.blelibrary.ble;

/**
 * This class sets various static property values for Bluetooth
 * Created by liulei on 2016/11/29.
 */

public class BleStates {

    public static final int NotInit = 2000;
    public static final int InitAlready = 2001;
    public static final int NotSupportBLE = 2005;
    public static final int BluetoothNotOpen = 2006;
    public static final int NotAvailable = 2007;
    public static final int BlePermissionError = 2008;
    public static final int NotFindDevice = 2009;
    public static final int InvalidAddress = 2010;

    public static final int ScanAlready = 2020;
    public static final int ScanStopAlready = 2021;
    public static final int ScanFrequentlyError = 2022;
    public static final int ScanError = 2023;

    public static final int ConnectedAlready = 2030;
    public static final int ConnectFailed = 2031;
    public static final int ConnectError = 2032;
    public static final int ConnectException = 2033;
    public static final int MaxConnectNumException = 2034;

    public static final int NoService = 2040;
    public static final int DeviceNull = 2041;
    public static final int ConnectTimeOut = 2044;
    public static final int NotInitUuid = 2045;

    public static final int CharaUuidNull = 2050;

}
