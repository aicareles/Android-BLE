package com.example.admin.mybledemo.annotation;

import android.view.View;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by jerry on 2018/6/13.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@EventInterface(lisenterSetter = "setOnLongClickListener",
        lisenterType = View.OnLongClickListener.class,callbackMethod = "onLongClick")
public @interface OnLongClick {
    int[] value() default -1;
}
