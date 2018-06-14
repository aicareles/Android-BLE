package com.example.admin.mybledemo.aop.aspect;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.example.admin.mybledemo.MyApplication;
import com.example.admin.mybledemo.activity.BaseActivity;
import com.example.admin.mybledemo.aop.Permission;
import com.example.admin.mybledemo.utils.PermissionUtils;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;

/**
 * Created by jerry on 2018/6/13.
 */

@Aspect
public class SysPermissionAspect {

    @Around("execution(@com.example.admin.mybledemo.aop.Permission * *(..)) && @annotation(permission)")
    public void aroundJoinPoint(final ProceedingJoinPoint joinPoint, final Permission permission) throws Throwable {
        final Activity ac = MyApplication.getInstance().getCurActivity();
        new AlertDialog.Builder(ac)
                .setTitle("提示")
                .setMessage("为了应用可以正常使用，请您点击确认申请权限。")
                .setNegativeButton("取消", null)
                .setPositiveButton("允许", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        PermissionUtils.INSTANCE.requestPermission(ac, new String[]{permission.value()}, "请求设备权限", new PermissionUtils.GrantedResult() {
                           @Override
                           public void onResult(boolean granted) {
                               if (granted){
                                   try {
                                       joinPoint.proceed();//获得权限，执行原方法
                                   } catch (Throwable e) {
                                       e.printStackTrace();
                                   }
                               }else {
                                   Log.e("SysPermissionAspect", "onResult: false");
                               }
                           }
                       });
                    }
                })
                .create()
                .show();
    }
}
