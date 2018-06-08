package com.example.admin.mybledemo.utils;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import com.example.admin.mybledemo.MyApplication;

public final class ToastUtil {

    private static Toast mToast;

    public static void showToast(String text) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), text, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(text);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static void showToast( int paramInt) {
        if (mToast == null) {
            mToast = Toast.makeText(MyApplication.getInstance(), paramInt, Toast.LENGTH_SHORT);
        } else {
            mToast.setText(paramInt);
            mToast.setDuration(Toast.LENGTH_SHORT);
        }
        mToast.show();
    }

    public static void cancelToast() {
        if (mToast != null) {
            mToast.cancel();
        }
    }
    public static void info(Activity paramActivity, int paramInt) {
        Toast.makeText(paramActivity, paramInt, Toast.LENGTH_SHORT).show();
    }

    public static void info(Activity paramActivity, String paramString) {
        Toast.makeText(paramActivity, paramString, Toast.LENGTH_SHORT).show();
    }
    public static void info(Context context, String paramString) {
        Toast.makeText(context, paramString, Toast.LENGTH_SHORT).show();
    }
}