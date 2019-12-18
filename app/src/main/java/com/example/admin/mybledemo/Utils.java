package com.example.admin.mybledemo;

import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Method;

import cn.com.superLei.aoparms.annotation.Async;

public class Utils {

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

    /**
     * 拷贝OTA升级文件到SD卡
     */
    @Async
    public static void copyOtaFile(final Context context, final String path) {
        //判断是否存在ota文件
        if (SPUtils.get(context, Constant.SP.OTA_FILE_EXIST, false))return;
        File file = new File(path);
        if (!file.exists()) {
            file.mkdir();
        }
        File newFile = new File(path + Constant.Constance.OTA_FILE_PATH);
        copyFileToSD(context, Constant.Constance.OTA_FILE_PATH, newFile.getAbsolutePath());
        SPUtils.put(context, Constant.SP.OTA_FILE_EXIST, true);
    }

    private static void copyFileToSD(Context context, String assetPath, String strOutFileName) {
        try {
            InputStream myInput;
            OutputStream myOutput = new FileOutputStream(strOutFileName);
            myInput = context.getAssets().open(assetPath);
            byte[] buffer = new byte[1024];
            int length = myInput.read(buffer);
            while (length > 0) {
                myOutput.write(buffer, 0, length);
                length = myInput.read(buffer);
            }
            myOutput.flush();
            myInput.close();
            myOutput.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static class SPUtils {

        /**
         * 保存在手机里面的文件名
         */
        public static final String FILE_NAME = "share_data";

        /**
         * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
         *
         * @param context
         * @param key
         * @param object
         */
        public static void put(Context context, String key, Object object) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = sp.edit();
            if (object instanceof String) {
                editor.putString(key, (String) object);
            } else if (object instanceof Integer) {
                editor.putInt(key, (Integer) object);
            } else if (object instanceof Boolean) {
                editor.putBoolean(key, (Boolean) object);
            } else if (object instanceof Float) {
                editor.putFloat(key, (Float) object);
            } else if (object instanceof Long) {
                editor.putLong(key, (Long) object);
            } else {
                editor.putString(key, object == null ? null : String.valueOf(object));
            }
            SharedPreferencesCompat.apply(editor);
        }

        /**
         * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
         *
         * @param context
         * @param key
         * @param defaultObject
         * @return
         */
        public static <T> T get(Context context, String key, T defaultObject) {
            SharedPreferences sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE);
            if (defaultObject instanceof String) {
                return (T) sp.getString(key, (String) defaultObject);
            } else if (defaultObject instanceof Integer) {
                return (T) Integer.valueOf(sp.getInt(key, (Integer) defaultObject));
            } else if (defaultObject instanceof Boolean) {
                return (T) Boolean.valueOf(sp.getBoolean(key, (Boolean) defaultObject));
            } else if (defaultObject instanceof Float) {
                return (T) Float.valueOf(sp.getFloat(key, (Float) defaultObject));
            } else if (defaultObject instanceof Long) {
                return (T) Long.valueOf(sp.getLong(key, (Long) defaultObject));
            } else {
                return (T) sp.getString(key, (String) defaultObject);
            }
        }
        /**
         * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
         *
         * @author zhy
         */
        private static class SharedPreferencesCompat {
            private static final Method sApplyMethod = findApplyMethod();

            /**
             * 反射查找apply的方法
             *
             * @return
             */
            @SuppressWarnings({"unchecked", "rawtypes"})
            private static Method findApplyMethod() {
                try {
                    Class clz = SharedPreferences.Editor.class;
                    return clz.getMethod("apply");
                } catch (NoSuchMethodException e) {
                    e.printStackTrace();
                }
                return null;
            }

            /**
             * 如果找到则使用apply执行，否则使用commit
             *
             * @param editor
             */
            public static void apply(SharedPreferences.Editor editor) {
                try {
                    if (sApplyMethod != null) {
                        sApplyMethod.invoke(editor);
                        return;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                editor.commit();
            }
        }

    }

}
