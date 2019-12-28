package com.example.admin.mybledemo;

import cn.com.heaton.blelibrary.ble.model.BleDevice;

public class BleRssiDevice {
    private BleDevice device;
    private int rssi;
    private long rssiUpdateTime;

    public BleRssiDevice(BleDevice device, int rssi) {
        this.device = device;
        this.rssi = rssi;
    }

    public BleDevice getDevice() {
        return device;
    }

    public void setDevice(BleDevice device) {
        this.device = device;
    }

    public int getRssi() {
        return rssi;
    }

    public void setRssi(int rssi) {
        this.rssi = rssi;
    }

    public long getRssiUpdateTime() {
        return rssiUpdateTime;
    }

    public void setRssiUpdateTime(long rssiUpdateTime) {
        this.rssiUpdateTime = rssiUpdateTime;
    }
}
