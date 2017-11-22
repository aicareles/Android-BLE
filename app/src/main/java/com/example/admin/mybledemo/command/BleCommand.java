package com.example.admin.mybledemo.command;

import java.util.LinkedList;

/**
 *
 * Created by LiuLei on 2017/11/6.
 */

public class BleCommand implements ConcreteCommand {

    private LinkedList<Command> commands = new LinkedList<>();

    @Override
    public void add(Command command) {
        commands.add(command);
    }

    @Override
    public void remove(Command command) {
        commands.remove(command);
    }

    @Override
    public void execute() {
        for (Command command : commands){
            command.execute();
        }
    }
}
