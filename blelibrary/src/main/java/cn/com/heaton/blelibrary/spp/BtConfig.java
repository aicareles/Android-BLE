package cn.com.heaton.blelibrary.spp;

import java.util.UUID;

/**
 *
 * Created by LiuLei on 2017/9/14.
 */

public class BtConfig {

    public final static String                 UUID_SECURE_TEXT    = "00001101-0000-1000-8000-00805F9B34FB";//spp加密服务UUID字符串
    public final static String                 UUID_INSECURE_TEXT  = "00001101-0000-1000-8000-00805F9B34FB";//spp未加密服务UUID字符串
    public final static UUID UUID_SECURE         = UUID.fromString(UUID_SECURE_TEXT);//spp加密服务UUID
    public final static UUID                   UUID_INSECURE       = UUID.fromString(UUID_INSECURE_TEXT);//spp未加密服务UUID

}
