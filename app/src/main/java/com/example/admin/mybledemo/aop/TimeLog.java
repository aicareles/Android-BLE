package com.example.admin.mybledemo.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 打印方法耗时Log注解，
 * 通过aop切片的方式在编译期间织入源代码中
 * Created by jerry on 2018/6/13.
 */

@Retention(RetentionPolicy.CLASS)
@Target({ElementType.CONSTRUCTOR, ElementType.METHOD})
public @interface TimeLog {
}
