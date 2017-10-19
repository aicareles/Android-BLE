package com.example.admin.mybledemo.activity;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.LLAnnotation;

public class IntroducedActivity extends AppCompatActivity {

    private static final String TAG = "IntroducedActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_introduce);

    }

}
