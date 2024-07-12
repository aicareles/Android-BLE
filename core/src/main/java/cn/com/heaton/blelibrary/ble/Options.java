package cn.com.heaton.blelibrary.ble;

/**
 * author: jerry
 * date: 21-1-9
 * email: superliu0911@gmail.com
 * des:
 */

import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.os.Build;
import androidx.annotation.IntRange;
import androidx.annotation.RequiresApi;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.callback.wrapper.BleWrapperCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.BleFactory;
import cn.com.heaton.blelibrary.ble.queue.reconnect.IReconnectStrategy;

/**
 * 蓝牙相关参数配置类
 */
public class Options {
    /**
     * 是否打印蓝牙日志
     */
    public boolean logBleEnable = true;
    /**
     * 日志TAG，用于过滤日志信息
     */
    public String logTAG = "AndroidBLE";
    /**
     * 是否抛出蓝牙异常
     */
    public boolean throwBleException = true;
    /**
     * 是否在蓝牙异常断开时自动连接
     */
    public boolean autoConnect = false;
    /**
     * 蓝牙连接超时时长
     */
    public long connectTimeout = 10 * 1000L;
    /**
     * 蓝牙扫描周期时长
     */
    public long scanPeriod = 10 * 1000L;
    /**
     * 服务绑定失败重试次数
     */
    public int serviceBindFailedRetryCount = 3;
    /**
     * 蓝牙连接失败重试次数
     */
    public int connectFailedRetryCount = 3;

    public int maxConnectNum = 7;
    /**
     * 是否过滤扫描设备
     */
    public boolean isIgnoreRepeat = false;

    public ScanFilter scanFilter;
    /**
     * 是否解析广播包  (发送接收广播包时可以打开)
     */
    public boolean isParseScanData = false;
    /**
     * 广播包,厂商id
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int manufacturerId = 65520; // 0xfff0

    /**
     * TODO 待优化(泛型)
     */
    private BleWrapperCallback bleWrapperCallback;

    private BleFactory factory;

    public Options setScanPeriod(long scanPeriod){
        this.scanPeriod = scanPeriod;
        return this;
    }

    public String getLogTAG() {
        return logTAG;
    }

    public Options setLogTAG(String logTAG) {
        this.logTAG = logTAG;
        return this;
    }

    public boolean isLogBleEnable() {
        return logBleEnable;
    }

    public Options setLogBleEnable(boolean logBleEnable) {
        this.logBleEnable = logBleEnable;
        return this;
    }

    public boolean isThrowBleException() {
        return throwBleException;
    }

    public Options setThrowBleException(boolean throwBleException) {
        this.throwBleException = throwBleException;
        return this;
    }

    public boolean isAutoConnect() {
        return autoConnect;
    }

    public Options setAutoConnect(boolean autoConnect) {
        this.autoConnect = autoConnect;
        return this;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public Options setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public long getScanPeriod() {
        return scanPeriod;
    }

    public int getServiceBindFailedRetryCount() {
        return serviceBindFailedRetryCount;
    }

    public Options setServiceBindFailedRetryCount(int serviceBindFailedRetryCount) {
        this.serviceBindFailedRetryCount = serviceBindFailedRetryCount;
        return this;
    }

    public int getConnectFailedRetryCount() {
        return connectFailedRetryCount;
    }

    public Options setConnectFailedRetryCount(@IntRange(from = 0, to = 5)int connectFailedRetryCount) {
        this.connectFailedRetryCount = connectFailedRetryCount;
        return this;
    }

    public int getMaxConnectNum() {
        return maxConnectNum;
    }

    public Options setMaxConnectNum(@IntRange(from = 1, to = 7)int maxConnectNum) {
        this.maxConnectNum = maxConnectNum;
        return this;
    }

    public boolean isIgnoreRepeat() {
        return isIgnoreRepeat;
    }

    public Options setIgnoreRepeat(boolean ignoreRepeat) {
        isIgnoreRepeat = ignoreRepeat;
        return this;
    }

    public ScanFilter getScanFilter() {
        return scanFilter;
    }

    public Options setScanFilter(ScanFilter scanFilter) {
        this.scanFilter = scanFilter;
        return this;
    }

    public boolean isParseScanData() {
        return isParseScanData;
    }

    public Options setParseScanData(boolean parseScanData) {
        isParseScanData = parseScanData;
        return this;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public int getManufacturerId() {
        return manufacturerId;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public Options setManufacturerId(int manufacturerId) {
        this.manufacturerId = manufacturerId;
        return this;
    }

    public BleWrapperCallback getBleWrapperCallback() {
        return bleWrapperCallback;
    }

    public Options setBleWrapperCallback(BleWrapperCallback bleWrapperCallback) {
        this.bleWrapperCallback = bleWrapperCallback;
        return this;
    }

    public BleFactory getFactory(){
        if (factory == null){
            factory = new BleFactory() {
                @Override
                public BleDevice create(String address, String name) {
                    return super.create(address, name);
                }
            };
        }
        return factory;
    }

    /**
     * 自定义device时，必须设置factory，不然会造成强制转换异常
     */
    public Options setFactory(BleFactory factory) {
        this.factory = factory;
        return this;
    }

    UUID[] uuid_services_extra = new UUID[]{};
    UUID uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");
    UUID uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    UUID uuid_read_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");
    UUID uuid_notify_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");
    UUID uuid_notify_desc = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb");

    UUID uuid_ota_service = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb");
    UUID uuid_ota_notify_cha = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16");
    UUID uuid_ota_write_cha = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16");

    public UUID[] getUuidServicesExtra() {
        return uuid_services_extra;
    }

    public Options setUuidServicesExtra(UUID[] uuid_services_extra) {
        this.uuid_services_extra = uuid_services_extra;
        return this;
    }

    public UUID getUuidService() {
        return uuid_service;
    }

    public Options setUuidService(UUID uuid_service) {
        this.uuid_service = uuid_service;
        return this;
    }

    public UUID getUuidWriteCha() {
        return uuid_write_cha;
    }

    public Options setUuidWriteCha(UUID uuid_write_cha) {
        this.uuid_write_cha = uuid_write_cha;
        return this;
    }

    public UUID getUuidReadCha() {
        return uuid_read_cha;
    }

    public Options setUuidReadCha(UUID uuid_read_cha) {
        this.uuid_read_cha = uuid_read_cha;
        return this;
    }

    public UUID getUuidNotifyCha() {
        return uuid_notify_cha;
    }

    public Options setUuidNotifyCha(UUID uuid_notify_cha) {
        this.uuid_notify_cha = uuid_notify_cha;
        return this;
    }

    public UUID getUuidNotifyDesc() {
        return uuid_notify_desc;
    }

    public Options setUuidNotifyDesc(UUID uuid_notify_desc) {
        this.uuid_notify_desc = uuid_notify_desc;
        return this;
    }

    public UUID getUuidOtaService() {
        return uuid_ota_service;
    }

    public Options setUuidOtaService(UUID uuid_ota_service) {
        this.uuid_ota_service = uuid_ota_service;
        return this;
    }

    public UUID getUuidOtaNotifyCha() {
        return uuid_ota_notify_cha;
    }

    public Options setUuidOtaNotifyCha(UUID uuid_ota_notify_cha) {
        this.uuid_ota_notify_cha = uuid_ota_notify_cha;
        return this;
    }

    public UUID getUuidOtaWriteCha() {
        return uuid_ota_write_cha;
    }

    public Options setUuidOtaWriteCha(UUID uuid_ota_write_cha) {
        this.uuid_ota_write_cha = uuid_ota_write_cha;
        return this;
    }

    public <T extends BleDevice> Ble<T> create(Context context){
        return create(context, null);
    }

    public <T extends BleDevice> Ble<T> create(Context context, Ble.InitCallback callback){
        return Ble.create(context, callback);
    }

}
