package com.example.admin.mybledemo.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.example.admin.mybledemo.R;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void ble(View view){
        startActivity(new Intent(MainActivity.this,BleActivity.class));
    }

    public void spp(View view){
        startActivity(new Intent(MainActivity.this,SppActivity.class));
    }
}
