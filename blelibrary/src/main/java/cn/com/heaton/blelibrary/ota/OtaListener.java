package cn.com.heaton.blelibrary.ota;

import cn.com.heaton.blelibrary.BleVO.BleDevice;

/**
 * Created by LiuLei on 2017/6/7.
 */

public interface OtaListener {
    void onWrite();

    void onChange(byte[] data);
}
