package cn.com.heaton.blelibrary.ble.exception;

/**
 * Created by LiuLei on 2017/10/19.
 */

public class BleWriteException extends BleException {

    private static final long serialVersionUID = -6886122979840622897L;

    public BleWriteException() {
    }

    public BleWriteException(String message) {
        super(message);
    }

    public BleWriteException(String s, Throwable ex) {
        super(s, ex);
    }
}
