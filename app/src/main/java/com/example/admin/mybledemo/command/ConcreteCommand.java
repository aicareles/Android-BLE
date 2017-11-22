package com.example.admin.mybledemo.command;

/**
 *
 * Created by LiuLei on 2017/11/6.
 */

public interface ConcreteCommand extends Command {

    void add(Command command);

    void remove(Command command);


}
