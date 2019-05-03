package cn.com.heaton.blelibrary;

import org.junit.Test;

import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;

import static org.junit.Assert.*;

/**
 * 测试用例
 * Created by jerry on 2018/10/15.
 */
public class BleInitTest {
    @Test
    public void testInit() throws Exception {
        Ble.Options options =  Ble.options()
                .setLogBleExceptions(true)
                .setThrowBleException(true)
                .setAutoConnect(false)
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)
                .setScanPeriod(12 * 1000)
                .setUuid_service(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))
                .setUuid_write_cha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"));
//                .create(new MockContext());//模拟器初始化bleAdapter会报错
        assertNotNull(options);
    }

}