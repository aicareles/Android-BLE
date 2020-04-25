package cn.com.heaton.blelibrary.ble.model;

import android.text.TextUtils;

import cn.com.heaton.blelibrary.ble.exception.BleWriteException;

/**
 * description $desc$
 * created by jerry on 2019/5/27.
 */
public class EntityData {

    private static final int DEFAULT_LENGTH = 20;
    //是否是自动发送模式
    private boolean autoWriteMode;
    //蓝牙设备地址
    private String address;
    //大数据字节数组对象
    private byte[]data;
    //每包大小
    private int packLength = DEFAULT_LENGTH;
    //每包发送间隔
    private long delay;
    //最后一包是否自动补零
    private boolean lastPackComplete;

    public EntityData(boolean autoWriteMode, String address, byte[] data, int packLength, long delay, boolean lastPackComplete) {
        this.autoWriteMode = autoWriteMode;
        this.address = address;
        this.data = data;
        this.packLength = packLength;
        this.delay = delay;
        this.lastPackComplete = lastPackComplete;
    }

    public EntityData(String address, byte[] data, int packLength, long delay, boolean lastPackComplete) {
        this(false, address, data, packLength, delay, false);
    }

    public EntityData(String address, byte[] data, int packLength, long delay) {
        this(false, address, data, packLength, delay, false);
    }

    public EntityData(){}

    public EntityData(String address, byte[] data, int packLength) {
        this(false, address, data, packLength, 0L, false);
    }

    public boolean isAutoWriteMode() {
        return autoWriteMode;
    }

    public void setAutoWriteMode(boolean autoWriteMode) {
        this.autoWriteMode = autoWriteMode;
    }

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

    public long getDelay() {
        return delay;
    }

    public void setDelay(long delay) {
        this.delay = delay;
    }

    public boolean isLastPackComplete() {
        return lastPackComplete;
    }

    public void setLastPackComplete(boolean lastPackComplete) {
        this.lastPackComplete = lastPackComplete;
    }

    public static class Builder {
        private boolean autoWriteMode;
        private String address;
        private byte[]data;
        private int packLength = DEFAULT_LENGTH;
        private long delay;
        private boolean lastPackComplete;

        public boolean isAutoWriteMode() {
            return autoWriteMode;
        }

        public Builder setAutoWriteMode(boolean autoWriteMode) {
            this.autoWriteMode = autoWriteMode;
            return this;
        }

        public String getAddress() {
            return address;
        }

        public Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public byte[] getData() {
            return data;
        }

        public Builder setData(byte[] data) {
            this.data = data;
            return this;
        }

        public int getPackLength() {
            return packLength;
        }

        public Builder setPackLength(int packLength) {
            this.packLength = packLength;
            return this;
        }

        public long getDelay() {
            return delay;
        }

        public Builder setDelay(long delay) {
            this.delay = delay;
            return this;
        }

        public boolean isLastPackComplete() {
            return lastPackComplete;
        }

        public Builder setLastPackComplete(boolean lastPackComplete) {
            this.lastPackComplete = lastPackComplete;
            return this;
        }

        public EntityData build(){
            return new EntityData(autoWriteMode, address, data, packLength, delay, lastPackComplete);
        }
    }

    public static void validParms(EntityData entityData) {
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
