package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mybledemo.command.Command;
import com.example.admin.mybledemo.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.StaticValue;
import com.example.admin.mybledemo.annotation.LLAnnotation;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.example.admin.mybledemo.utils.FileUtils;
import com.example.admin.mybledemo.utils.SPUtils;
import com.orhanobut.logger.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ota.OtaManager;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends BaseActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private String TAG = BleActivity.class.getSimpleName();

    @ViewInit(R.id.test)
    private Button mTest;
    @ViewInit(R.id.readRssi)
    private Button mReadRssi;
    @ViewInit(R.id.sendData)
    private Button mSend;
    @ViewInit(R.id.updateOta)
    private Button mUpdateOta;
    @ViewInit(R.id.listView)
    private ListView mListView;
    @ViewInit(R.id.connected_num)
    private TextView mConnectedNum;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private String path;

    @Override
    protected int getLayoutResource() {
        return R.layout.activity_ble;
    }

    @Override
    protected void onInitView() {
        requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_COARSE_LOCATION},
                "请求蓝牙相关权限", new GrantedResult() {
            @Override
            public void onResult(boolean granted) {
                if(granted){
                    //初始化蓝牙
                    initBle();
                }else {
                    finish();
                }
            }
        });

        initView();
    }

    @Override
    protected void initLinsenter() {
        mTest.setOnClickListener(this);
        mReadRssi.setOnClickListener(this);
        mSend.setOnClickListener(this);
        mUpdateOta.setOnClickListener(this);
        mListView.setOnItemClickListener(this);
    }

    private void initView() {
        setTitle("Ble界面");
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
    }

    @Override
    public void onClick(View v) {
        List<BleDevice> list = mBle.getConnetedDevices();
        if (list.size() == 0) {
            Toast.makeText(BleActivity.this, "请先连接设备", Toast.LENGTH_SHORT).show();
            return;
        }
        switch (v.getId()) {
            case R.id.test:
                if(mBle.isScanning()){
                    mBle.stopScan();
                }
                //根据自身需求传入需要在其他界面操作的蓝牙对象  这里测试取第一个设备对象
                BleDevice d = mBle.getConnetedDevices().get(0);
                startActivity(new Intent(BleActivity.this,TestActivity.class).putExtra("device",d));
                break;
            case R.id.readRssi:
                mBle.readRssi(mBle.getConnetedDevices().get(0), new BleReadRssiCallback<BleDevice>() {
                    @Override
                    public void onReadRssiSuccess(int rssi) {
                        super.onReadRssiSuccess(rssi);
                        Log.e(TAG, "onReadRssiSuccess: " + rssi);
                        Toast.makeText(BleActivity.this, "onReadRssiSuccess:"+ rssi, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case R.id.sendData:
                synchronized (mBle.getLocker()) {
                    for (BleDevice device : list) {
                        sendData(device);
                    }
                }
                break;
            case R.id.updateOta:
                //此处为了方便把OTA升级文件直接放到assets文件夹下，拷贝到/aceDownload/文件夹中  以便使用
                requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        "读写SD卡相关权限", new GrantedResult() {
                            @Override
                            public void onResult(boolean granted) {
                                if(granted){
                                    CopyAssetsToSD();
                                }else {
                                    Toast.makeText(BleActivity.this, "读写SD卡权限被拒绝,将会影响OTA升级功能哦！", Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                File file = new File(path + StaticValue.OTA_NEW_PATH);
                OtaManager mOtaManager = new OtaManager(BleActivity.this);
                boolean result = mOtaManager.startOtaUpdate(file, (BleDevice) mBle.getConnetedDevices().get(0), mBle);
                Log.e("OTA升级结果:", result + "");
                break;
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        final BleDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mBle.isScanning()) {
            mBle.stopScan();
        }
        if (device.isConnected()) {
            mBle.disconnect(device, connectCallback);
        } else if (!device.isConnectting()) {
            mBle.connect(device, connectCallback);
        }
    }

    private void initBle() {
        mBle = Ble.getInstance();
        Ble.Options options = new Ble.Options();
        options.logBleExceptions = true;//设置是否输出打印蓝牙日志
        options.throwBleException = true;//设置是否抛出蓝牙异常
        options.autoConnect = false;//设置是否自动连接
        options.scanPeriod = 12 * 1000;//设置扫描时长
        options.connectTimeout = 10 * 1000;//设置连接超时时长
        options.uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");//设置主服务的uuid
        options.uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");//设置可写特征的uuid
//        options.uuid_read_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");//设置可读特征的uuid
        mBle.init(getApplicationContext(), options);

        checkBle();

        mBle.startScan(scanCallback);
    }

    /*检查蓝牙是否支持及打开*/
    private void checkBle() {
        // 检查设备是否支持BLE4.0
        if (!mBle.isSupportBle(this)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBle.isBleEnable()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        }
    }

    /*发送数据*/
    public void sendData(BleDevice device) {
        boolean result = mBle.write(device, changeLevelInner(1), new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BluetoothGattCharacteristic characteristic) {
                Toast.makeText(BleActivity.this, "发送数据成功", Toast.LENGTH_SHORT).show();
            }
        });
        if (!result) {
            Log.e(TAG, "changeLevelInner: " + "发送数据失败!");
        }
    }
    
    /*主动读取数据*/
    public void read(BleDevice device){
        boolean result = mBle.read(device, new BleReadCallback<BleDevice>() {
            @Override
            public void onReadSuccess(BluetoothGattCharacteristic characteristic) {
                super.onReadSuccess(characteristic);
                byte[] data = characteristic.getValue();
                Log.w(TAG, "onReadSuccess: "+Arrays.toString(data));
            }
        });
        if(!result){
            Log.d(TAG, "读取数据失败!");
        }
    }

    //播放音乐
    public byte[] changeLevelInner(int play) {
        byte[] data = new byte[Command.qppDataSend.length];
        System.arraycopy(Command.qppDataSend, 0, data, 0, data.length);
        data[6] = 0x03;
        data[7] = (byte) play;
        Logger.e("data:" + Arrays.toString(data));
        return data;
    }

//    播放音乐
//    public byte[] changeLevelInner() {
//        int var = 0xAA51;//左邊是高位  右邊是低位
//        byte[] data = new byte[2];
//        data[0] =  (byte)((var>>8) & 0xff);
//        data[1] =  (byte)(var & 0xff);
//        Logger.e("data:" + Arrays.toString(data));
//        Logger.e("data11:" + Integer.toHexString(var));
//        return data;
//    }

    //扫描的回调
    BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            synchronized (mBle.getLocker()) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mLeDeviceListAdapter.addDevice(device);
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                });
            }
        }

        @Override
        public void onStop() {
            super.onStop();
        }
    };

    /*连接的回调*/
    private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            if (device.isConnected()) {
                setNotify(device);
            }
            Log.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
            setConnectedNum();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Toast.makeText(BleActivity.this, "连接异常，异常状态码:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    //这里是全局的
    byte[] mBuff = new byte[4096];
    int mReadCount = 0;

    /*设置通知的回调*/
    private void setNotify(BleDevice device) {
         /*连接成功后，设置通知*/
        mBle.startNotify(device, new BleNotiftCallback<BleDevice>() {
            @Override
            public void onChanged(BluetoothGattCharacteristic characteristic) {
                UUID uuid = characteristic.getUuid();
                Log.e(TAG, "onChanged: "+uuid.toString());
                Log.e(TAG, "onChanged: " + Arrays.toString(characteristic.getValue()));
            }

            @Override
            public void onReady(BleDevice device) {
                Log.e(TAG, "onReady: ");
            }

            @Override
            public void onServicesDiscovered(BluetoothGatt gatt) {
                Log.e(TAG, "onServicesDiscovered is success ");
            }

            @Override
            public void onNotifySuccess(BluetoothGatt gatt) {
                Log.e(TAG, "onNotifySuccess is success ");
            }
        });
    }

    /*更新当前连接设备的数量*/
    private void setConnectedNum() {
        if (mBle != null) {
            Log.e("mConnectedNum", "已连接的数量：" + mBle.getConnetedDevices().size() + "");
            for (BleDevice device : mBle.getConnetedDevices()) {
                Log.e("device", "设备地址：" + device.getBleAddress());
            }
            mConnectedNum.setText(getString(R.string.lined_num) + mBle.getConnetedDevices().size());
        }
    }

    /*拷贝OTA升级文件到SD卡*/
    private void CopyAssetsToSD() {
        //判断是否是第一次进入   默认第一次进入
        if (!SPUtils.get(BleActivity.this, StaticValue.IS_FIRST_RUN, true)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取SD卡路径
                path = Environment.getExternalStorageDirectory()
                        + "/aceDownload/";
                File file = new File(path);
                // 如果SD卡目录不存在创建
                if (!file.exists()) {
                    file.mkdir();
                }

                final File newFile = new File(path + StaticValue.OTA_NEW_PATH);
                final File oldFile = new File(path + StaticValue.OTA_OLD_PATH);
                try {
                    FileUtils.copyBigDataToSD(BleActivity.this, StaticValue.OTA_NEW_PATH, newFile.getAbsolutePath());
                    FileUtils.copyBigDataToSD(BleActivity.this, StaticValue.OTA_OLD_PATH, oldFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //设置程序非第一次进入
                SPUtils.put(BleActivity.this, StaticValue.IS_FIRST_RUN, false);
            }
        }).start();
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
                reScan();
                break;
            case R.id.menu_stop:
                Logger.e("点击了停止扫描按钮");
                if (mBle != null) {
                    mBle.stopScan();
                }
                break;
            case R.id.menu_connect_all:
                Logger.e("点击了连接全部设备按钮");
                if (mBle != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mBle.connect(device, connectCallback);
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                Logger.e("点击了断开全部设备按钮");
                if (mBle != null) {
                    ArrayList<BleDevice> list = mBle.getConnetedDevices();
                    for (BleDevice device : list) {
                        mBle.disconnect(device);
                    }
                }
                break;
            case R.id.menu_introduced:
                startActivity(new Intent(BleActivity.this, IntroducedActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    //重新扫描
    private void reScan() {
        if (mBle != null && !mBle.isScanning()) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
            mBle.startScan(scanCallback);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            if (mBle != null) {
                mBle.startScan(scanCallback);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mBle != null) {
            mBle.destory(getApplicationContext());
        }
    }

}
