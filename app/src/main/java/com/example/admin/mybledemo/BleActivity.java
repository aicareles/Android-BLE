package com.example.admin.mybledemo;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.mybledemo.adapter.ScanAdapter;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.BleUtils;
import cn.com.superLei.aoparms.annotation.Permission;
import cn.com.superLei.aoparms.annotation.PermissionDenied;
import cn.com.superLei.aoparms.annotation.PermissionNoAskDenied;
import cn.com.superLei.aoparms.common.permission.AopPermissionUtils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends AppCompatActivity {
    private String TAG = BleActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_PERMISSION_WRITE = 3;
    public static final int REQUEST_GPS = 4;
    private LinearLayout llBlutoothAdapterTip;
    private TextView tvAdapterStates;
    private SwipeRefreshLayout swipeLayout;
    private FloatingActionButton floatingActionButton;
    private RecyclerView recyclerView;
    private FilterView filterView;
    private ScanAdapter adapter;
    private List<BleRssiDevice> bleRssiDevices;
    private Ble<BleDevice> ble = Ble.getInstance();
    private ObjectAnimator animator;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        initView();
        initAdapter();
        initLinsenter();
        requestPermission();
    }

    private void initAdapter() {
        bleRssiDevices = new ArrayList<>();
        adapter = new ScanAdapter(this, bleRssiDevices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.addItemDecoration(new DividerItemDecoration(this,DividerItemDecoration.VERTICAL));
        recyclerView.getItemAnimator().setChangeDuration(300);
        recyclerView.getItemAnimator().setMoveDuration(300);
        recyclerView.setAdapter(adapter);
    }

    private void initView() {
        llBlutoothAdapterTip = findViewById(R.id.ll_adapter_tip);
        swipeLayout = findViewById(R.id.swipeLayout);
        tvAdapterStates = findViewById(R.id.tv_adapter_states);
        recyclerView = findViewById(R.id.recyclerView);
        floatingActionButton = findViewById(R.id.floatingButton);
        filterView = findViewById(R.id.filterView);
        filterView.init(this);
    }

    private void initLinsenter() {
        tvAdapterStates.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
            }
        });
        floatingActionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                rescan();
            }
        });
        swipeLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                swipeLayout.setRefreshing(false);
                rescan();
            }
        });

    }

    //请求权限
    @Permission(value = {Manifest.permission.ACCESS_COARSE_LOCATION},
            requestCode = REQUEST_PERMISSION_LOCATION,
            rationale = "需要蓝牙相关权限")
    public void requestPermission() {
        checkBlueStatus();
        initBleStatus();
    }

    @PermissionDenied
    public void permissionDenied(int requestCode, List<String> denyList) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            Log.e(TAG, "permissionDenied>>>:定位权限被拒 " + denyList.toString());
        } else if (requestCode == REQUEST_PERMISSION_WRITE) {
            Log.e(TAG, "permissionDenied>>>:读写权限被拒 " + denyList.toString());
        }
    }

    @PermissionNoAskDenied
    public void permissionNoAskDenied(int requestCode, List<String> denyNoAskList) {
        if (requestCode == REQUEST_PERMISSION_LOCATION) {
            Log.e(TAG, "permissionNoAskDenied 定位权限被拒>>>: " + denyNoAskList.toString());
        } else if (requestCode == REQUEST_PERMISSION_WRITE) {
            Log.e(TAG, "permissionDenied>>>:读写权限被拒>>> " + denyNoAskList.toString());
        }
        AopPermissionUtils.showGoSetting(this, "为了更好的体验，建议前往设置页面打开权限");
    }

    //监听蓝牙开关状态
    private void initBleStatus() {
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                BleLog.i(TAG, "onBluetoothStatusOn: 蓝牙是否打开>>>>:" + isOn);
                llBlutoothAdapterTip.setVisibility(isOn?View.GONE:View.VISIBLE);
                if (isOn){
                    checkGpsStatus();
                }else {
                    if (ble.isScanning()) {
                        ble.stopScan();
                    }
                }
            }
        });
    }

    //检查蓝牙是否支持及打开
    private void checkBlueStatus() {
        if (!ble.isSupportBle(this)) {
            Utils.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!ble.isBleEnable()) {
            llBlutoothAdapterTip.setVisibility(View.VISIBLE);
        }else {
            checkGpsStatus();
        }
    }

    private void checkGpsStatus(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
                && !BleUtils.isGpsOpen(BleActivity.this)){
            new AlertDialog.Builder(BleActivity.this)
                    .setTitle("提示")
                    .setMessage("为了更精确的扫描到Bluetooth LE设备,请打开GPS定位")
                    .setPositiveButton("确定", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                        startActivityForResult(intent,REQUEST_GPS);
                    })
                    .setNegativeButton("取消", null)
                    .create()
                    .show();
        }else {
            ble.startScan(scanCallback);
        }
    }

    private void rescan() {
        if (ble != null && !ble.isScanning()) {
            bleRssiDevices.clear();
            adapter.notifyDataSetChanged();
            ble.startScan(scanCallback);
        }
    }

    private BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            BleLog.i(TAG, "onLeScan: " + device.getBleName());
            if (TextUtils.isEmpty(device.getBleName())) return;
            synchronized (ble.getLocker()) {
                for (int i = 0; i < bleRssiDevices.size(); i++) {
                    BleRssiDevice rssiDevice = bleRssiDevices.get(i);
                    BleDevice bleDevice = rssiDevice.getDevice();
                    if (TextUtils.equals(bleDevice.getBleAddress(), device.getBleAddress())){
                        if (rssiDevice.getRssi() != rssi && System.currentTimeMillis()-rssiDevice.getRssiUpdateTime() >1000L){
                            rssiDevice.setRssiUpdateTime(System.currentTimeMillis());
                            rssiDevice.setRssi(rssi);
                            adapter.notifyItemChanged(i);
                        }
                        return;
                    }
                }
                BleRssiDevice rssiDevice = new BleRssiDevice(device, ScanRecord.parseFromBytes(scanRecord), rssi);
                bleRssiDevices.add(rssiDevice);
                adapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStart() {
            super.onStart();
            startBannerLoadingAnim();
        }

        @Override
        public void onStop() {
            super.onStop();
            stopBannerLoadingAnim();
        }
    };

    public void startBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_loading);
        animator = ObjectAnimator.ofFloat(floatingActionButton, "rotation", 0, 360);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setDuration(800);
        animator.setInterpolator(new LinearInterpolator());
        animator.start();
    }

    public void stopBannerLoadingAnim() {
        floatingActionButton.setImageResource(R.drawable.ic_bluetooth_audio_black_24dp);
        animator.cancel();
        floatingActionButton.setRotation(0);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_introduced:
                startActivity(new Intent(BleActivity.this, IntroducedActivity.class));
                break;
            case R.id.menu_share:
                Utils.shareAPK(this);
                break;
            case R.id.menu_contribute:
                ImageView imageView = new ImageView(this);
                imageView.setImageResource(R.drawable.wechat);
                new AlertDialog.Builder(BleActivity.this)
                        .setTitle("打赏/联系作者")
                        .setView(imageView)
                        .create()
                        .show();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
            ble.startScan(scanCallback);
        }else if (requestCode == REQUEST_GPS){

        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        /*if (ble != null) {
            ble.released();
            scanCallback = null;
            connectCallback = null;
            writeEntityCallback = null;
            bleNotiftCallback = null;
        }*/
    }

}
