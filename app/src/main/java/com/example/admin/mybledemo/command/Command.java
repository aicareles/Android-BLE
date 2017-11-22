package com.example.admin.mybledemo.command;

/**
 * 设备命令
 */
public interface Command {

    public static byte qppDataSend[] = {'T','R',0x16,0x06,'R',0x05,0x00,0x00,0x00,0x00,0x00,0x00,0x00}; // 发送数据的数组

    void execute();

}
