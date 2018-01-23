package cn.com.heaton.blelibrary.ble.factory;

import cn.com.heaton.blelibrary.ble.request.IMessage;

/**
 *
 * Created by LiuLei on 2017/12/28.
 */

public abstract class IRequestGenerator {
    public abstract <R extends IMessage>IMessage generateRequest(Class<R> clazz) throws Exception;
}
