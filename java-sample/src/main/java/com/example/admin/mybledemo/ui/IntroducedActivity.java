package com.example.admin.mybledemo.ui;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.View;
import android.widget.TextView;

import com.example.admin.mybledemo.R;

public class IntroducedActivity extends AppCompatActivity {

    private static final String TAG = "IntroducedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);

        TextView introduction = findViewById(R.id.introduction);
        String html = "温馨提示：\n" +
                "1、有部分Android手机（6.0及以上）连接BLE蓝牙必须先打开GPS定位（具体原因可自行搜索）\n" +
                "2、蓝牙连接错误的状态码可参考库中BleStates类中的详细说明。" +"https://github.com/aicareles/Android-BLE/blob/master/core/src/main/java/cn/com/heaton/blelibrary/ble/BleStates.java"+"\n"+
                "如下：\n" +
                "Mcu连接断开或者是信号弱等原因断开（错误码：2033）\n" +
                "连接失败（错误码：2031）\n" +
                "状态错误（错误码：2032）\n" +
                "等等。\n" +
                "3、该库最新依赖请参照README.md文件中说明\n" +
                "4、DEMO仅供参考，并不作为唯一标准（可能会产生问题），若发现库中错误可通过GitHub提交给作者进行修复\n"+
                "https://github.com/aicareles/Android-BLE";
        introduction.setText(html);
        introduction.setAutoLinkMask(Linkify.ALL);
        introduction.setMovementMethod(LinkMovementMethod.getInstance());

    }

}
