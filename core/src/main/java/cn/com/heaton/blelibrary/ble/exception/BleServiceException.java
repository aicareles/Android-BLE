package cn.com.heaton.blelibrary.ble.exception;

/**
 * Created by LiuLei on 2017/10/19.
 */

public class BleServiceException extends BleException {


    private static final long serialVersionUID = 3871013343556227444L;

    public BleServiceException() {
    }

    public BleServiceException(String message) {
        super(message);
    }

    public BleServiceException(String s, Throwable ex) {
        super(s, ex);
    }
}
