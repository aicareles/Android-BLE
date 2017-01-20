/*
 * Copyright (c) 2015 Zhang Hai <Dreaming.in.Code.ZH@Gmail.com>
 * All Rights Reserved.
 */

package com.example.admin.mybledemo.utils;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.StaticValue;

public class ThemeUtils {

    private ThemeUtils() {}

    private static final int[] THEME_IDS = new int[] {
            R.style.ThemeBaseLight,
            R.style.ThemeBaseDark
    };

    public static int getThemeId(Context context) {
        int index = SPUtils.get(context, StaticValue.KEY_THEME, 0);
        Log.e("ThemeUtils",index+"");
        return THEME_IDS[index];
    }

    public static void applyTheme(Activity activity) {
        activity.setTheme(getThemeId(activity));
    }
}
