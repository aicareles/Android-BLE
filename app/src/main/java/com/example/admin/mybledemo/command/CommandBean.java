package com.example.admin.mybledemo.command;

import com.example.admin.mybledemo.C;

import java.util.List;

/**
 * 蓝牙控制命令对象
 * Created by jerry on 2018/10/5.
 */

public class CommandBean {

    private int random_roll;//随机滚码
    private int command_type;//命令类型
    private int delay = 100;//ms
    private int speed;//0-200   值 100 表示小车停止（以 100 为速度零点） 小于 100 表示小车后退，数值越小，后退越快，最小值为 0。 大于 100 表示小车前进，数值越大，前进越快，最大值为 200。
    private int turn;//值 0 表示小车直走，不转弯。 值 1 表示小车左转弯。 值 2 表示小车右转弯。

    private int frame_sum;//
    private int current_frame;//
    private int order_type;//值 0 表示小车执行一次此组合控制命令，不存储此命令。 值 1 表示小车执行一次此组合控制命令，并且把此命令存储进 Flash。 值 2 表示小车不执行此组合控制命令，但是把此命令存储进 Flash。 值 3 表示小车不执行此组合控制命令，并且如果 Flash 内部含有当前组合命 令，将其从 Flash 删除掉。
    private int last_length;//表示字符串在此字节后的剩余字节长度
    private List<Byte> order_list;//组合命令集合  包含方向和时间

    private int music_type;
    private int play_status;
    private short music_index;
    private int music_volum;//0-30

    public CommandBean() {
    }

    public CommandBean setCarCommand(int speed, int turn) {
        this.command_type = C.Command.BLE_CAR_COMMAND;
        this.speed = speed;
        this.turn = turn;
        return this;
    }

    public CommandBean setOrderCommand(int frame_sum, int current_frame, List<Byte> list) {
        this.command_type = C.Command.BLE_ORDER_COMMAND;
        this.frame_sum = frame_sum;
        this.current_frame = current_frame;
        this.order_type = 0x01;// 默认1
        this.last_length = list.size();
        this.order_list = list;
        return this;
    }

    public CommandBean setMscCommand(int music_type, int play_status, short music_index) {
        this.command_type = C.Command.BLE_MUSIC_COMMAND;
        this.music_type = music_type;
        this.play_status = play_status;
        this.music_index = music_index;
        return this;
    }

    public CommandBean setVolumeCommand(int music_type, int music_volum) {
        this.command_type = C.Command.BLE_MUSIC_COMMAND;
        this.music_type = music_type;
        this.music_volum = music_volum;
        return this;
    }

    public int getRandom_roll() {
        return 0;
    }

    public int getCommand_type() {
        return command_type;
    }

    public void setCommand_type(int command_type) {
        this.command_type = command_type;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public int getSpeed() {
        return speed;
    }

    public void setSpeed(int speed) {
        this.speed = speed;
    }

    public int getTurn() {
        return turn;
    }

    public void setTurn(int turn) {
        this.turn = turn;
    }

    public int getFrame_sum() {
        return frame_sum;
    }

    public void setFrame_sum(int frame_sum) {
        this.frame_sum = frame_sum;
    }

    public int getCurrent_frame() {
        return current_frame;
    }

    public void setCurrent_frame(int current_frame) {
        this.current_frame = current_frame;
    }

    public int getOrder_type() {
        return order_type;
    }

    public void setOrder_type(int order_type) {
        this.order_type = order_type;
    }

    public int getLast_length() {
        return last_length;
    }

    public void setLast_length(int last_length) {
        this.last_length = last_length;
    }

    public List<Byte> getOrder_list() {
        return order_list;
    }

    public void setOrder_list(List<Byte> order_list) {
        this.order_list = order_list;
    }

    public int getMusic_type() {
        return music_type;
    }

    public void setMusic_type(int music_type) {
        this.music_type = music_type;
    }

    public int getPlay_status() {
        return play_status;
    }

    public void setPlay_status(int play_status) {
        this.play_status = play_status;
    }

    public short getMusic_index() {
        return music_index;
    }

    public void setMusic_index(short music_index) {
        this.music_index = music_index;
    }

    public int getMusic_volum() {
        return music_volum;
    }

    public void setMusic_volum(int music_volum) {
        this.music_volum = music_volum;
    }
}
