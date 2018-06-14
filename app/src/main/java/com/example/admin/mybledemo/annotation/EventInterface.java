package com.example.admin.mybledemo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 对所有的事件点击 进行扩展
 * Created by jerry on 2018/6/13.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
public @interface EventInterface {

    /**
     * 设置监听的方法
     * @return
     */
    String lisenterSetter();

    /**
     * 事件类型
     * @return
     */
    Class lisenterType();

    /**
     * 回调方法
     * 事件被触发后，执行回调方法名称
     */
    String callbackMethod();
}
