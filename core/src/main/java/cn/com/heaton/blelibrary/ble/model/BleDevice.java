package cn.com.heaton.blelibrary.ble.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.RestrictTo;

import cn.com.heaton.blelibrary.ble.Ble;

/**
 * Created by LiuLei on 2016/11/26.
 */
public class BleDevice implements Parcelable {

    public static final int DISCONNECT = 0;
    public static final int CONNECTING = 1;
    public static final int CONNECTED = 2;

    public final static String TAG = BleDevice.class.getSimpleName();
    private static final long serialVersionUID = -2576082824642358033L;

    /**
     * 连接状态
     * 2503 未连接状态
     * 2504 正在连接
     * 2505 连接成功
     */
    private int mConnectionState = DISCONNECT;

    /*蓝牙地址*/
    private String mBleAddress;

    /*蓝牙名称*/
    private String mBleName;

    /*蓝牙重命名名称（别名）*/
    private String mBleAlias;

    /*是否自动连接*/
    private boolean mAutoConnect = Ble.options().autoConnect;//The default is not automatic connection

    /*是否正在自动重连*/
    private boolean isAutoConnecting = false;

    /*解析后的广播包数据*/
    private ScanRecord scanRecord;

    /**
     * Use the address and name of the BluetoothDevice object
     * to construct the address and name of the {@code BleDevice} object
     */
    protected BleDevice(String address, String name) {
        this.mBleAddress = address;
        this.mBleName = name;
    }

    protected BleDevice(Parcel in) {
        mConnectionState = in.readInt();
        mBleAddress = in.readString();
        mBleName = in.readString();
        mBleAlias = in.readString();
        mAutoConnect = in.readByte() != 0;
        isAutoConnecting = in.readByte() != 0;
    }

    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public static final Creator<BleDevice> CREATOR = new Creator<BleDevice>() {
        @Override
        public BleDevice createFromParcel(Parcel in) {
            return new BleDevice(in);
        }

        @Override
        public BleDevice[] newArray(int size) {
            return new BleDevice[size];
        }
    };

    public boolean isConnected() {
        return mConnectionState == CONNECTED;
    }

    public boolean isConnecting() {
        return mConnectionState == CONNECTING;
    }

    public boolean isDisconnected() {
        return mConnectionState == DISCONNECT;
    }

    public boolean isAutoConnect() {
        return mAutoConnect;
    }

    public void setAutoConnect(boolean mAutoConnect) {
        this.mAutoConnect = mAutoConnect;
    }

    public boolean isAutoConnecting() {
        return isAutoConnecting;
    }

    public void setAutoConnecting(boolean autoConnecting) {
        isAutoConnecting = autoConnecting;
    }

    public int getConnectionState() {
        return mConnectionState;
    }

    public void setConnectionState(int state) {
        mConnectionState = state;
    }

    public String getBleAddress() {
        return mBleAddress;
    }

    public void setBleAddress(String mBleAddress) {
        this.mBleAddress = mBleAddress;
    }

    public String getBleName() {
        return mBleName;
    }

    public void setBleName(String mBleName) {
        this.mBleName = mBleName;
    }

    public String getBleAlias() {
        return mBleAlias;
    }

    public void setBleAlias(String mBleAlias) {
        this.mBleAlias = mBleAlias;
    }

    public ScanRecord getScanRecord() {
        return scanRecord;
    }

    public void setScanRecord(ScanRecord scanRecord) {
        this.scanRecord = scanRecord;
    }

    @Override
    public String toString() {
        return "BleDevice{" +
                "mConnectionState=" + mConnectionState +
                ", mBleAddress='" + mBleAddress + '\'' +
                ", mBleName='" + mBleName + '\'' +
                ", mBleAlias='" + mBleAlias + '\'' +
                ", mAutoConnect=" + mAutoConnect +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mConnectionState);
        dest.writeString(mBleAddress);
        dest.writeString(mBleName);
        dest.writeString(mBleAlias);
        dest.writeByte((byte) (mAutoConnect ? 1 : 0));
        dest.writeByte((byte) (isAutoConnecting ? 1 : 0));
    }
}
