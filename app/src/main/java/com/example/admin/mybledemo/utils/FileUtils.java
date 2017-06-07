package com.example.admin.mybledemo.utils;

import android.content.Context;
import android.util.Log;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by admin on 2017/1/16.
 */

public class FileUtils {

    public static void copyBigDataToSD(Context context, String assetPath, String strOutFileName) throws IOException
    {
        InputStream myInput;
        OutputStream myOutput = new FileOutputStream(strOutFileName);
        myInput = context.getAssets().open(assetPath);
        byte[] buffer = new byte[1024];
        int length = myInput.read(buffer);
        while(length > 0)
        {
            myOutput.write(buffer, 0, length);
            length = myInput.read(buffer);
        }
        Log.e("FlieUtils==","复制到sd卡完成");

        myOutput.flush();
        myInput.close();
        myOutput.close();
    }
}
