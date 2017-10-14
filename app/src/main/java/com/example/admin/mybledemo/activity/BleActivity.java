package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
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

import com.example.admin.mybledemo.Command;
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

import cn.com.heaton.blelibrary.ble.BleConfig;
import cn.com.heaton.blelibrary.ble.BleLisenter;
import cn.com.heaton.blelibrary.ble.BleManager;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ota.OtaManager;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends BaseActivity {

    private String TAG = BleActivity.class.getSimpleName();

    @ViewInit(R.id.sendData)
    private Button mSend;
    @ViewInit(R.id.updateOta)
    private Button mUpdateOta;
    @ViewInit(R.id.listView)
    private ListView mListView;
    @ViewInit(R.id.connected_num)
    private TextView mConnectedNum;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BleManager<BleDevice> mManager;
    private String path;

    private BleLisenter mLisenter = new BleLisenter() {
        @Override
        public void onStart() {
            super.onStart();
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onStop() {
            super.onStop();
            //可以选择性实现该方法   不需要则不用实现
            invalidateOptionsMenu();
        }

        @Override
        public void onConnectTimeOut() {
            super.onConnectTimeOut();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplication(), R.string.connect_timeout, Toast.LENGTH_SHORT).show();
                    synchronized (mManager.getLocker()) {
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            Logger.e("onLeScan");
//                            //可以选择性的根据scanRecord蓝牙广播包进行过滤
//                            如下  此处注释（根据你们产品的广播进行过滤或者根据产品的特定name或者address进行过滤也可以）
//                            if(!BleConfig.matchProduct(scanRecord)){
//                                return;
//                            }
            Toast.makeText(BleActivity.this,"ssss",Toast.LENGTH_SHORT).show();
            synchronized (mManager.getLocker()) {
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
        public void onConnectionChanged(final BleDevice device) {
            Logger.e("onConnectionChanged" + device.getConnectionState() + device.isConnected());
            setConnectedNum();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        if (device.getBleAddress().equals(mLeDeviceListAdapter.getDevice(i).getBleAddress())) {
                            if (device.isConnected()) {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.CONNECTED);
                                Toast.makeText(BleActivity.this, R.string.line_success, Toast.LENGTH_SHORT).show();
                            } else if (device.isConnectting()) {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.CONNECTING);
                            } else {
                                mLeDeviceListAdapter.getDevice(i).setConnectionState(BleConfig.BleStatus.DISCONNECT);
                                Toast.makeText(BleActivity.this, R.string.line_disconnect, Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                    synchronized (mManager.getLocker()) {
                        mLeDeviceListAdapter.notifyDataSetChanged();
                    }
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt) {
            super.onServicesDiscovered(gatt);
            //可以选择性实现该方法   不需要则不用实现  库中已设置Notify
        }

        @Override
        public void onReady(BluetoothDevice device) {
            super.onReady(device);
            Logger.e("onReady===+++++++可以写入数据了");
//            changeLevelInner(device.getAddress());
        }

        @Override
        public void onChanged(BluetoothGattCharacteristic characteristic) {
            Logger.e("data===" + Arrays.toString(characteristic.getValue()));
            Toast.makeText(BleActivity.this,"收到MCU返回数据："+Arrays.toString(characteristic.getValue()),Toast.LENGTH_SHORT).show();
            //可以选择性实现该方法   不需要则不用实现
            //硬件mcu 返回数据
        }

        @Override
        public void onWrite(BluetoothGatt gatt) {
            //可以选择性实现该方法   不需要则不用实现
        }

        @Override
        public void onRead(BluetoothDevice device) {
            super.onRead(device);
            //可以选择性实现该方法   不需要则不用实现
            Logger.e("onRead");
        }

        @Override
        public void onDescriptorWriter(BluetoothGatt gatt) {
            super.onDescriptorWriter(gatt);
            //可以选择性实现该方法   不需要则不用实现
        }
    };

    public boolean changeLevelInner(String address) {
//        byte[] data = new byte[Command.ComSyncColorLen];
//        System.arraycopy(Command.ComSyncColor, 0, data, 0, Command.ComSyncColor.length);
//        data[4] = (byte) (color & 0xff);
//        data[5] = (byte) ((color >> 8) & 0xff);
//        data[6] = (byte) ((color >> 16) & 0xff);
//        data[7] = (byte) ((color >> 24) & 0xff);
        boolean result = mManager.sendData(address, sendData(1));
        Logger.e("result==" + result);
        Toast.makeText(BleActivity.this,"发送数据成功",Toast.LENGTH_SHORT).show();
        return result;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);

        //初始化注解  替代findViewById
        LLAnnotation.viewInit(this);
        //初始化蓝牙
        initBle();
        initView();

        //此处为了方便把OTA升级文件直接放到assets文件夹下，拷贝到/aceDownload/文件夹中  以便使用
        requestPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE},
                "需要读写权限", new GrantedResult() {
                    @Override
                    public void onResult(boolean granted) {
                        if (granted) {
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
                                    CopyAssetsToSD();
                                }
                            }).start();
                        }
                    }
                });
    }

    private void CopyAssetsToSD() {
        //判断是否是第一次进入   默认第一次进入
        if (!SPUtils.get(this, StaticValue.IS_FIRST_RUN, true)) {
            return;
        }
        final File newFile = new File(path + StaticValue.OTA_NEW_PATH);
        final File oldFile = new File(path + StaticValue.OTA_OLD_PATH);
        requestPermission(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                "需要读写权限", new GrantedResult() {
                    @Override
                    public void onResult(boolean granted) {
                        if (granted) {
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        FileUtils.copyBigDataToSD(BleActivity.this, StaticValue.OTA_NEW_PATH, newFile.getAbsolutePath());
                                        FileUtils.copyBigDataToSD(BleActivity.this, StaticValue.OTA_OLD_PATH, oldFile.getAbsolutePath());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            }).start();
                        }
                    }
                });
        //设置程序非第一次进入
        SPUtils.put(this, StaticValue.IS_FIRST_RUN, false);
    }

    private void initBle() {
        //配置设备可自动重连
        BleConfig.isAutoConnect = true;
        //设置主服务、写入特征、设置通知的描述uuid等（根据你自己需求加  比如单个设备可能有多个服务  如电池服务等  此处只添加一个主服务）

        //设置主服务uuid
        BleConfig.setUuidServiceText("0000fee9-0000-1000-8000-00805f9b34fb");
        //设置写入特征uuid
        BleConfig.setUuidCharacteristicText("d44bc439-abfd-45a2-b575-925416129600");
        //设置通知uuid（此处由于库中进行遍历  即获取所有的通知进行设置 所以可以不用写）
        BleConfig.setUuidNotifyText("");
        //设置写入通知的描述uuid（其实也可以不用写  库中并没有进行遍历所有的描述   所以此处用于进行过滤）
        BleConfig.setUuidDescriptorText("00002902-0000-1000-8000-00805f9b34fb");
        try {
            mManager = BleManager.getInstance(this);
            mManager.registerBleListener(mLisenter);
            boolean result = false;
            if (mManager != null) {
                result = mManager.startService();
                if (!mManager.isBleEnable()) {//蓝牙未打开
                    mManager.turnOnBlueTooth(this);
                } else {//已打开
                    requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, getString(R.string.ask_permission), new GrantedResult() {
                        @Override
                        public void onResult(boolean granted) {
                            if (!granted) {
                                finish();
                            } else {
                                //开始扫描
                                mManager.scanLeDevice(true);
                            }
                        }
                    });
                }
            }
            if (!result) {
                Logger.e("服务绑定失败");
                if (mManager != null) {
                    mManager.startService();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //播放音乐
    public byte[] sendData(int play) {
        byte[] data = new byte[Command.qppDataSend.length];
        System.arraycopy(Command.qppDataSend, 0, data, 0, data.length);
        data[6] = 0x03;
        data[7] = (byte) play;
        Logger.e("data:" + Arrays.toString(data));
        return data;
    }

    public void junp(View view){
        startActivity(new Intent(BleActivity.this,TestActivity.class));
    }

    private void initView() {
        setTitle("Ble界面");
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                List<BleDevice> list = mManager.getConnetedDevices();
                if (list.size() > 0) {
                    synchronized (mManager.getLocker()) {
                        for (BleDevice device : list) {
                            changeLevelInner(device.getBleAddress());
                        }
                    }
                }
            }
        });
        mUpdateOta.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mManager.getConnetedDevices().size() > 0) {
                    File file = new File(path + StaticValue.OTA_NEW_PATH);
                    OtaManager mOtaManager = new OtaManager(BleActivity.this);
                    boolean result = mOtaManager.startOtaUpdate(file, mManager.getConnetedDevices().get(0), mManager);
                    Log.e("OTA升级结果:", result + "");
                } else {
                    Toast.makeText(BleActivity.this, "请先连接设备", Toast.LENGTH_SHORT).show();
                }
            }
        });
        // Initializes list view adapter.
        if (mManager != null) {
            mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        }
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
                if (mManager.isScanning()) {
                    mManager.scanLeDevice(false);
                }
                if (device.isConnected()) {
                    mManager.disconnect(device);
                } else if(!device.isConnectting()){
                    mManager.connect(device);
                }

            }
        });
    }


    private void setConnectedNum() {
        if (mManager != null) {
            Log.e("mConnectedNum", "已连接的数量：" + mManager.getConnetedDevices().size() + "");
            for (BleDevice device : mManager.getConnetedDevices()) {
                Log.e("device", "设备地址：" + device.getBleAddress());
            }
            mConnectedNum.setText(getString(R.string.lined_num) + mManager.getConnetedDevices().size());
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
                if (mManager != null && !mManager.isScanning()) {
                    mLeDeviceListAdapter.clear();
                    for(BleDevice device : mManager.getConnetedDevices()){
                        mLeDeviceListAdapter.addDevice(device);
                    }
                    mManager.getScanBleDevice().clear();
                    mManager.scanLeDevice(true);
                }
                break;
            case R.id.menu_stop:
                Logger.e("点击了停止扫描按钮");
                if (mManager != null) {
                    mManager.scanLeDevice(false);
                }
                break;
            case R.id.menu_connect_all:
                Logger.e("点击了连接全部设备按钮");
                if (mManager != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mManager.connect(device);
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                Logger.e("点击了断开全部设备按钮");
                if (mManager != null) {
                    ArrayList<BleDevice> list = mManager.getConnetedDevices();
                    for (BleDevice device : list) {
                        mManager.disconnect(device);
                    }
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == BleManager.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            if (mManager != null) {
                mManager.scanLeDevice(true);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mManager != null && !mManager.isScanning()) {
            mLeDeviceListAdapter.clear();
            mManager.clear();
            mManager.scanLeDevice(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mManager != null) {
            mManager.scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mManager != null) {
            mManager.unService();
            mManager.unRegisterBleListener(mLisenter);
        }
    }

}
