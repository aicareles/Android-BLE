package cn.com.heaton.blelibrary.ble.exception;

/**
 * Created by LiuLei on 2017/10/19.
 */

public class BlePermissionException extends BleException {

    private static final long serialVersionUID = -6791491579172360482L;

    public BlePermissionException() {
    }

    public BlePermissionException(String message) {
        super(message);
    }

    public BlePermissionException(String s, Throwable ex) {
        super(s, ex);
    }
}
