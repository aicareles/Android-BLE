package com.example.admin.mybledemo.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by LiuLei on 2017/6/9.
 */

@Target(value = ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface ViewInit {
    int value() default -1;
}
