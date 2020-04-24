package com.i502tech.appkotlin

import android.bluetooth.BluetoothGattCharacteristic
import cn.com.heaton.blelibrary.ble.Ble
import cn.com.heaton.blelibrary.ble.BleLog
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback
import cn.com.heaton.blelibrary.ble.model.BleDevice
import cn.com.heaton.blelibrary.ble.utils.ByteUtils

/**
 * 蓝牙协议类
 * Created by jerry on 2018/10/5.
 */

object AppProtocol {
    val TAG = "AppProtocol"

    private val bleDeviceBleWriteCallback = object :BleWriteCallback<BleDevice>() {
        override fun onWriteSuccess(device: BleDevice?, characteristic: BluetoothGattCharacteristic?) {
            BleLog.w(TAG, "onWriteSuccess: ")
        }

        override fun onWiteFailed(device: BleDevice?, failedCode: Int) {
            BleLog.e(TAG, "onWiteFailed:$failedCode")
        }

    }

    private fun write(device: BleDevice, data: ByteArray) {
        val list = Ble.getInstance<BleDevice>().connetedDevices
        if (null != list && list.size > 0) {
            Ble.getInstance<BleDevice>().write(device, data, bleDeviceBleWriteCallback)
        }
    }

    /**
     * 手机端 APP 通过 BLE 发送字符串控制小车移动    格式：：”XC0123456789ABCD”（共 16 个 byte，共 1 帧）  周期发送，以间隔周期 100ms 发送一次控制命令给小车。
     */
    fun sendCarMoveCommand(device: BleDevice, commandBean: CommandBean) {
        val data = ByteArray(16)
        data[0] = commandBean.getRandom_roll().toByte()
        data[1] = commandBean.command_type.toByte()
        data[2] = (commandBean.speed and 0xff).toByte()
        data[3] = commandBean.turn.toByte()
        write(device, data)
    }

    /**
     * 手机端 APP 通过 BLE 发送小车的组合控制命令   格式：”XO0123456789ABCD”（共 16 个 byte，帧数不定）
     */
    fun sendCarCmdCommand(device: BleDevice, commandBean: CommandBean) {
        val data = ByteArray(16)
        data[0] = commandBean.getRandom_roll().toByte()
        data[1] = commandBean.command_type.toByte()
        data[2] = commandBean.frame_sum.toByte()
        data[3] = commandBean.current_frame.toByte()
        data[4] = 0x01
        data[5] = commandBean.last_length.toByte()
        for (i in 0 until commandBean.order_list!!.size) {
            data[6 + i] = commandBean.order_list!![i]
        }
        write(device, data)
    }

    /**
     * 手机端 APP 通过 BLE 发送字符串控制音乐 格式：”XM0123456789ABCD”（共 16 个 byte，共 1 帧）
     */
    fun sendCarMscCommand(device: BleDevice, commandBean: CommandBean) {
        val data = ByteArray(16)
        data[0] = commandBean.getRandom_roll().toByte()
        data[1] = commandBean.command_type.toByte()
        data[2] = commandBean.music_type.toByte()
        data[3] = commandBean.play_status.toByte()
        data[4] = ByteUtils.short2Bytes(commandBean.music_index)[0]
        data[5] = ByteUtils.short2Bytes(commandBean.music_index)[1]
        write(device, data)
    }

    /**
     * 发送本地音乐或者TF卡音乐的音量值
     */
    fun sendMusicVolume(device: BleDevice, commandBean: CommandBean) {
        val data = ByteArray(16)
        data[2] = commandBean.music_type.toByte()
        data[3] = 0x04
        data[6] = (commandBean.music_volum and 0xff).toByte()
        write(device, data)
    }
}
