package com.example.admin.mybledemo.activity;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mybledemo.BtDeviceAdapter;
import com.example.admin.mybledemo.Command;
import com.example.admin.mybledemo.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.LLAnnotation;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.orhanobut.logger.Logger;

import java.util.ArrayList;
import java.util.Arrays;

import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.spp.BtDevice;
import cn.com.heaton.blelibrary.spp.BtManager;
import cn.com.heaton.blelibrary.spp.BtUtils;

/**
 * Activity for scanning and displaying available Bluetooth devices.
 */
public class SppActivity extends BaseActivity {

    private String TAG = SppActivity.class.getSimpleName();
    public final static int OPEN_BLUETH = 0x89;//请求打开蓝牙

    @ViewInit(R.id.listView)
    private ListView mListView;
    @ViewInit(R.id.connected_num)
    private TextView mConnectedNum;
    private BtDeviceAdapter mBtAdapter;
    private BtManager mBtManager;
    private boolean isScanning = false;//是否正在扫描

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spp);

        //初始化注解  替代findViewById
        LLAnnotation.viewInit(this);
        //初始化蓝牙
        initBle();
        initView();

    }

    private void initBle() {
        try {
            mBtManager = new BtManager(this, mBtListener);
            mBtManager.setSecure(true);
            //如果没有打开蓝牙，则弹出请打开蓝牙
            if (mBtManager.getAdapter() != null && !mBtManager.getAdapter().isEnabled()) {
                Intent enableBlueIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBlueIntent, OPEN_BLUETH);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BtManager.BtDeviceListener mBtListener = new BtManager.BtDeviceListener() {
        @Override
        public void onStateChanged(int state, BtDevice btDevice) {
            setConnectedNum();
            if (mBtAdapter != null) {
                mBtAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onDevicesChanged() {
            Log.e(TAG, "onDevicesChanged: " + "设备连接状态改变");
            if (mBtAdapter != null) {
                mBtAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onError(int errorCode, BtDevice btDevice) {

        }

        @Override
        public void onRead(byte[] buffer, BtDevice btDevice) {

        }

        @Override
        public void onWrite(byte[] buffer, BtDevice btDevice) {

        }

        @Override
        public void onFound(BtDevice btDevice) {
            synchronized (mBtManager.getLocker()) {
                mBtAdapter.addDevice(btDevice);
                mBtAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStartScan() {
            isScanning = true;
        }

        @Override
        public void onStopScan() {
            isScanning = false;
        }
    };

    //播放音乐
    boolean lock = false;//默认关
    public void sendData(View view) {
        if (mBtManager.getConnectedDevices().size() == 0) {//若当前没有连接设备则直接返回
            Toast.makeText(SppActivity.this,"请连接设备后重试",Toast.LENGTH_SHORT).show();
            return;
        }
        lock = !lock;
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] oc = new byte[6];
                oc[0] = 0;    //包的索引
                oc[1] = 4;    //包的长度
                oc[2] = 'A';
                oc[3] = 'T';
                oc[4] = 'E';
                oc[5] = (byte) (lock ? '1': '0');
                boolean result = mBtManager.getConnectedDevices().get(0).sendOnePacket(oc, 10, true);
                Log.e(TAG, "sendData: "+result);
            }
        }).start();
    }

    private void initView() {
        setTitle("SPP界面");
        mConnectedNum = (TextView) findViewById(R.id.connected_num);

        // Initializes list view adapter.
        if (mBtAdapter == null) {
            mBtAdapter = new BtDeviceAdapter(this);
        }
        mListView.setAdapter(mBtAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BtDevice device = mBtAdapter.getDevice(position);
                if (device == null) return;
                if (isScanning) {
                    mBtManager.cancelDiscovery();
                }
                if (device.isConnected()) {
                    device.close();
                } else {
                    mBtManager.addDevice(device);
//                    BtUtils.pair(device);
                    mBtManager.connect(device);
                }
            }
        });
    }


    private void setConnectedNum() {
        if (mBtManager != null) {
            Log.e("mConnectedNum", "已连接的数量：" + mBtManager.getConnectedDevices().size() + "");
            for (BtDevice device : mBtManager.getConnectedDevices()) {
                Log.e("device", "设备地址：" + device.getAddress());
            }
            mConnectedNum.setText(getString(R.string.lined_num) + mBtManager.getConnectedDevices().size());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                Logger.e("点击了扫描按钮");
                if (mBtManager != null && !isScanning) {
                    mBtAdapter.clear();
//                    mBtManager.release();
                    mBtManager.startDiscovery();
                }
                break;
            case R.id.menu_stop:
                Logger.e("点击了停止扫描按钮");
                if (mBtManager != null) {
                    mBtManager.cancelDiscovery();
                }
                break;
            case R.id.menu_connect_all:
                Logger.e("点击了连接全部设备按钮");
                if (mBtManager != null) {
                    for (int i = 0; i < mBtAdapter.getCount(); i++) {
                        BtDevice device = mBtAdapter.getDevice(i);
                        mBtManager.connect(device);
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                Logger.e("点击了断开全部设备按钮");
                if (mBtManager != null) {
                    ArrayList<BtDevice> list = (ArrayList<BtDevice>) mBtManager.getConnectedDevices();
                    for (BtDevice device : list) {
                        device.close();
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == OPEN_BLUETH) {
            if (resultCode == RESULT_OK) {
                //TO DO SOMETHING   打开蓝牙后的操作
                if (mBtManager != null && !isScanning) {
                    mBtManager.startDiscovery();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mBtManager != null && !isScanning) {
            mBtAdapter.clear();
//            mBtManager.release();
            mBtManager.startDiscovery();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBtManager != null) {
            mBtManager.cancelDiscovery();
        }
        mBtAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

}
