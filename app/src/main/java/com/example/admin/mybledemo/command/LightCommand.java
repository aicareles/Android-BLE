package com.example.admin.mybledemo.command;

/**
 *
 * Created by LiuLei on 2017/11/6.
 */

public class LightCommand implements Command {

    public int command; //命令类型
    public int value;//命令值

    public LightCommand(int command,int value){
        this.command = command;
        this.value = value;
    }

    @Override
    public void execute() {

    }
}
