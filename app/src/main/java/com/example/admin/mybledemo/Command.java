package com.example.admin.mybledemo;

/**
 * 设备命令
 */
public class Command {
    public static byte[] ComSyncColor    = {'C', 'O', 'L', 'R'};
    public static int    ComSyncColorLen = 8;
    public static byte[] ComSyncMode     = {'M', 'O', 'D', 'E'};
    public static int    ComSyncModeLen  = 6;
    public static byte[] ComSyncLevel    = {'L', 'E', 'V', 'L'};
    public static int    ComSyncLevelLen = 6;

    //关闭灯光的命令
    public static byte[] ComSyncLight    = {'A', 'L', 'P', 'A'};
    public static int    ComSyncLightLen = 6;

    //来电的命令
    public static byte[] ComSyncCalling    = {'C', 'A', 'L', 'L'};
    public static int    ComSyncCallingLen = 15;


    public static final int MODE_GRADUAL_START      = 0;  //渐变颜色的索引开始
    public static final int MODE_RED_GRADUAL        = 0;    //红色渐变
    public static final int MODE_GREEN_GRADUAL      = 1;   //绿色渐变
    public static final int MODE_BLUE_GRADUAL       = 2;    //蓝色渐变
    public static final int MODE_YELLOW_GRADUAL     = 3;  //黄色渐变
    public static final int MODE_CYAN_GRADUAL       = 4;    //青色渐变
    public static final int MODE_PURPLE_GRADUAL     = 5;  //紫色渐变
    public static final int MODE_WHITE_GRADUAL      = 6;   //白色渐变
    public static final int MODE_RED_GREEN_GRADUAL  = 7;//红绿渐变
    public static final int MODE_RED_BLIE_GRADUAL   = 8; //红蓝渐变
    public static final int MODE_GREEN_BLUEG_RADUAL = 9;//绿蓝渐变
    public static final int MODE_COLORFUL_GRADUAL   = 10;//七彩渐变
    public static final int MODE_COLORFUL_SWITCH    = 11; //七彩跳变
    //频闪
    public static final int MODE_FLASH_START        = 12;    //频闪颜色的索引开始
    public static final int MODE_COLORFUL_FLASH     = 12; //七彩频闪
    public static final int MODE_RED_FLASH          = 13;       //红色频闪
    public static final int MODE_GREEN_FLASH        = 14;   //绿色频闪
    public static final int MODE_BLUE_FLASH         = 15;    //蓝色频闪
    public static final int MODE_YELLOW_FLASH       = 16;  //黄色频闪
    public static final int MODE_CYAN_FLASH         = 17;    //青色频闪
    public static final int MODE_PURPLE_FLASH       = 18;  //紫色频闪
    public static final int MODE_WHITE_FLASH        = 19;   //白色频闪
}
