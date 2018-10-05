package com.example.admin.mybledemo.command;

import android.bluetooth.BluetoothGattCharacteristic;

import com.example.admin.mybledemo.utils.ByteUtils;

import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;

/**
 * 蓝牙协议类
 * Created by jerry on 2018/10/5.
 */

public class AppProtocol {

    private static void write(BleDevice device, byte[] data){
        List<BleDevice> list = Ble.getInstance().getConnetedDevices();
        if(null != list && list.size() > 0){
            Ble.getInstance().write(device, getWriteData(data), bleDeviceBleWriteCallback);
        }
    }

    private static BleWriteCallback<BleDevice> bleDeviceBleWriteCallback = new BleWriteCallback<BleDevice>() {
        @Override
        public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
//            Log.e(TAG, "onWriteSuccess: ");
        }
    };

    public static byte[] getWriteData(byte[] data){
//        aes.cipher(data, data);//加密
        return data;
    }

    /**
     * 手机端 APP 通过 BLE 发送字符串控制小车移动    格式：：”XC0123456789ABCD”（共 16 个 byte，共 1 帧）  周期发送，以间隔周期 100ms 发送一次控制命令给小车。
     */
    public static void sendCarMoveCommand(BleDevice device, CommandBean commandBean){
        byte[] data = new byte[16];
        data[0] = (byte) commandBean.getRandom_roll();
        data[1] = (byte) commandBean.getCommand_type();
        data[2] = (byte) (commandBean.getSpeed() & 0xff);
        data[3] = (byte) commandBean.getTurn();
        write(device, data);
    }

    /**
     * 手机端 APP 通过 BLE 发送小车的组合控制命令   格式：”XO0123456789ABCD”（共 16 个 byte，帧数不定）
     */
    public static void sendCarCmdCommand(BleDevice device, CommandBean commandBean){
        byte[] data = new byte[16];
        data[0] = (byte) commandBean.getRandom_roll();
        data[1] = (byte) commandBean.getCommand_type();
        data[2] = (byte) commandBean.getFrame_sum();
        data[3] = (byte) commandBean.getCurrent_frame();
        data[4] = 0x01;
        data[5] = (byte) commandBean.getLast_length();
        for (int i=0; i<commandBean.getOrder_list().size(); i++){
            data[6+i] = commandBean.getOrder_list().get(i);
        }
        write(device, data);
    }

    /**
     * 手机端 APP 通过 BLE 发送字符串控制音乐 格式：”XM0123456789ABCD”（共 16 个 byte，共 1 帧）
     */
    public static void sendCarMscCommand(BleDevice device, CommandBean commandBean){
        byte[] data = new byte[16];
        data[0] = (byte) commandBean.getRandom_roll();
        data[1] = (byte) commandBean.getCommand_type();
        data[2] = (byte) commandBean.getMusic_type();
        data[3] = (byte) commandBean.getPlay_status();
        data[4] = ByteUtils.short2Bytes(commandBean.getMusic_index())[0];
        data[5] = ByteUtils.short2Bytes(commandBean.getMusic_index())[1];
        write(device, data);
    }

    /**
     * 发送本地音乐或者TF卡音乐的音量值
     */
    public static void sendMusicVolume(BleDevice device, CommandBean commandBean){
        byte[] data = new byte[16];
        data[2] = (byte) commandBean.getMusic_type();
        data[3] = 0x04;
        data[6] = (byte) (commandBean.getMusic_volum() & 0xff);
        write(device, data);
    }
}
