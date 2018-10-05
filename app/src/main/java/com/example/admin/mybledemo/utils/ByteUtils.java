package com.example.admin.mybledemo.utils;

/**
 * 字节的转换
 * Created by admin on 2017/4/27.
 */

public class ByteUtils {
    //将字节数组转换为short类型，即统计字符串长度
    public static short bytes2Short2(byte[] b) {
        short i = (short) (((b[1] & 0xff) << 8) | b[0] & 0xff);
        return i;
    }
    //将字节数组转换为16进制字符串
    public static String BinaryToHexString(byte[] bytes) {
        String hexStr = "0123456789ABCDEF";
        String result = "";
        String hex = "";
        for (byte b : bytes) {
            hex = String.valueOf(hexStr.charAt((b & 0xF0) >> 4));
            hex += String.valueOf(hexStr.charAt(b & 0x0F));
            result += hex + " ";
        }
        return result;
    }

    //3.short转换为byte数组
    public static byte[] short2Bytes(short value) {
        byte[] data = new byte[2];
        data[0] = (byte) (value >> 8 & 0xff);
        data[1] = (byte) (value & 0xFF);
        return data;
    }

    /**
     * 将int转化成byte[]
     *
     * @param res 要转化的整数
     * @return 对应的byte[]
     */
    public static byte[] int2byte(int res) {
        byte[] targets = new byte[4];
        targets[0] = (byte) (res & 0xff);// 最低位
        targets[1] = (byte) ((res >> 8) & 0xff);// 次低位
        targets[2] = (byte) ((res >> 16) & 0xff);// 次高位
        targets[3] = (byte) (res >>> 24);// 最高位,无符号右移。
        return targets;
    }

    /**
     * 将byte[]转化成int
     * @param res 要转化的byte[]
     * @return 对应的整数
     */
    public static int byte2int(byte[] res) {
        int targets = (res[0] & 0xff) | ((res[1] << 8) & 0xff00) | ((res[2] << 24) >>> 8) | (res[3] << 24);
        return targets;
    }
}
