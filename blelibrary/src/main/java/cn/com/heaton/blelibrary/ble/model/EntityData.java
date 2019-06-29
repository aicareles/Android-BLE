package cn.com.heaton.blelibrary.ble.model;

import android.text.TextUtils;

import cn.com.heaton.blelibrary.ble.exception.BleWriteException;

/**
 * description $desc$
 * created by jerry on 2019/5/27.
 */
public class EntityData {

    private static final int DEFAULT_LENGTH = 20;
    private String address;
    private byte[]data;
    private int packLength = DEFAULT_LENGTH;
    private int delay;

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public byte[] getData() {
        if(data == null){
            data = new byte[0];
        }
        return data;
    }

    public void setData(byte[] data) {
        this.data = data;
    }

    public int getPackLength() {
        return packLength;
    }

    public void setPackLength(int packLength) {
        this.packLength = packLength;
    }

    public int getDelay() {
        return delay;
    }

    public void setDelay(int delay) {
        this.delay = delay;
    }

    public static void validParms(EntityData entityData) throws BleWriteException {
        String exception = "";
        if (TextUtils.isEmpty(entityData.address)){
            exception = "ble address isn't null";
        }
        if (entityData.data == null){
           exception = "ble data isn't null";
        }
        if (entityData.packLength <= 0){
            exception = "The data length per packet cannot be less than 0";
        }
        if (!TextUtils.isEmpty(exception)){
            throw new BleWriteException(exception);
        }
    }
}
