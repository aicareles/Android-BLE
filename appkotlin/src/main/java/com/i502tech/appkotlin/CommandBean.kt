package com.i502tech.appkotlin

/**
 * 蓝牙控制命令对象
 * Created by jerry on 2018/10/5.
 */

class CommandBean {
    internal var BLE_CAR_COMMAND: Int = 'C'.toInt()
    internal var BLE_ORDER_COMMAND: Int = 'O'.toInt()
    internal var BLE_MUSIC_COMMAND: Int = 'M'.toInt()

    private val random_roll: Int = 0//随机滚码
    var command_type: Int = 0//命令类型
    var delay = 100//ms
    var speed: Int = 0//0-200   值 100 表示小车停止（以 100 为速度零点） 小于 100 表示小车后退，数值越小，后退越快，最小值为 0。 大于 100 表示小车前进，数值越大，前进越快，最大值为 200。
    var turn: Int = 0//值 0 表示小车直走，不转弯。 值 1 表示小车左转弯。 值 2 表示小车右转弯。

    var frame_sum: Int = 0//
    var current_frame: Int = 0//
    var order_type: Int = 0//值 0 表示小车执行一次此组合控制命令，不存储此命令。 值 1 表示小车执行一次此组合控制命令，并且把此命令存储进 Flash。 值 2 表示小车不执行此组合控制命令，但是把此命令存储进 Flash。 值 3 表示小车不执行此组合控制命令，并且如果 Flash 内部含有当前组合命 令，将其从 Flash 删除掉。
    var last_length: Int = 0//表示字符串在此字节后的剩余字节长度
    var order_list: List<Byte>? = null//组合命令集合  包含方向和时间

    var music_type: Int = 0
    var play_status: Int = 0
    var music_index: Short = 0
    var music_volum: Int = 0//0-30

    fun setCarCommand(speed: Int, turn: Int): CommandBean {
        this.command_type = BLE_CAR_COMMAND
        this.speed = speed
        this.turn = turn
        return this
    }

    fun setOrderCommand(frame_sum: Int, current_frame: Int, list: List<Byte>): CommandBean {
        this.command_type = BLE_ORDER_COMMAND
        this.frame_sum = frame_sum
        this.current_frame = current_frame
        this.order_type = 0x01// 默认1
        this.last_length = list.size
        this.order_list = list
        return this
    }

    fun setMscCommand(music_type: Int, play_status: Int, music_index: Short): CommandBean {
        this.command_type = BLE_MUSIC_COMMAND
        this.music_type = music_type
        this.play_status = play_status
        this.music_index = music_index
        return this
    }

    fun setVolumeCommand(music_type: Int, music_volum: Int): CommandBean {
        this.command_type = BLE_MUSIC_COMMAND
        this.music_type = music_type
        this.music_volum = music_volum
        return this
    }

    fun getRandom_roll(): Int {
        return 0
    }
}
