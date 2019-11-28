package cn.com.heaton.blelibrary.ble.exception;

import java.io.Serializable;


/**
 *
 * Created by LiuLei on 2017/10/19.
 */

public class BleException extends RuntimeException implements Serializable{

    private static final long serialVersionUID = -3677084962477320584L;

    private Throwable ex;

    public BleException(){
        super();
    }

    public BleException(String message){
        super(message);
    }

    public BleException(String s, Throwable ex) {
        super(s, null);  //  Disallow initCause
        this.ex = ex;
    }

    public BleException(Throwable cause) {
        super(cause);
    }

    public Throwable getException() {
        return ex;
    }

    public Throwable getCause() {
        return ex;
    }
}
