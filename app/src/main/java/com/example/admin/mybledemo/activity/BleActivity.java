package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.example.admin.mybledemo.C;
import com.example.admin.mybledemo.adapter.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.ContentView;
import com.example.admin.mybledemo.annotation.OnClick;
import com.example.admin.mybledemo.annotation.OnItemClick;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.example.admin.mybledemo.aop.CheckConnect;
import com.example.admin.mybledemo.aop.SingleClick;
import com.example.admin.mybledemo.command.AppProtocol;
import com.example.admin.mybledemo.command.CommandBean;
import com.example.admin.mybledemo.utils.FileUtils;
import com.example.admin.mybledemo.utils.SPUtils;
import com.example.admin.mybledemo.utils.ToastUtil;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ota.OtaManager;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@ContentView( R.layout.activity_ble)
public class BleActivity extends BaseActivity{

    private String TAG = BleActivity.class.getSimpleName();

    @ViewInit(R.id.listView)
    ListView mListView;
    @ViewInit(R.id.connected_num)
    private TextView mConnectedNum;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private String path;

    @SingleClick //过滤重复点击
    @CheckConnect //检查是否连接
    @OnClick({R.id.test, R.id.readRssi, R.id.sendData, R.id.updateOta, R.id.requestMtu, R.id.sendEntityData})
    public void onClick(View view){
        switch (view.getId()){
            case R.id.test:
                if (mBle.isScanning()) {
                    mBle.stopScan();
                }
                startActivity(new Intent(BleActivity.this, TestActivity.class));
                break;
            case R.id.readRssi:
                mBle.readRssi(mBle.getConnetedDevices().get(0), new BleReadRssiCallback<BleDevice>() {
                    @Override
                    public void onReadRssiSuccess(int rssi) {
                        super.onReadRssiSuccess(rssi);
                        Log.e(TAG, "onReadRssiSuccess: " + rssi);
                        ToastUtil.showToast("读取远程RSSI成功："+rssi);
                    }
                });
                break;
            case R.id.sendData:
                List<BleDevice> list = Ble.getInstance().getConnetedDevices();
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
                                if (granted) {
                                    CopyAssetsToSD();
                                } else {
                                    ToastUtil.showToast("读写SD卡权限被拒绝,将会影响OTA升级功能哦!");
                                }
                            }
                        });
                File file = new File(path + C.Constance.OTA_NEW_PATH);
                OtaManager mOtaManager = new OtaManager(BleActivity.this);
                boolean result = mOtaManager.startOtaUpdate(file, (BleDevice) mBle.getConnetedDevices().get(0), mBle);
                Log.e("OTA升级结果:", result + "");
                break;
            case R.id.requestMtu:
                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP){
                    //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
                    mBle.setMTU(mBle.getConnetedDevices().get(0).getBleAddress(), 96, new BleMtuCallback<BleDevice>() {
                        @Override
                        public void onMtuChanged(BleDevice device, int mtu, int status) {
                            super.onMtuChanged(device, mtu, status);
                            ToastUtil.showToast("最大支持MTU："+mtu);
                        }
                    });
                }else {
                    ToastUtil.showToast("设备不支持MTU");
                }
                break;
            case R.id.sendEntityData:
                try {
                    byte[]data = toByteArray(getAssets().open("WhiteChristmas.bin"));
                    //发送大数据量的包
                    mBle.writeEntity(mBle.getConnetedDevices().get(0), data, 20, 50, new BleWriteEntityCallback<BleDevice>() {
                        @Override
                        public void onWriteSuccess() {
                            L.e("writeEntity", "onWriteSuccess");
                        }

                        @Override
                        public void onWriteFailed() {
                            L.e("writeEntity", "onWriteFailed");
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            default:
                break;
        }
    }

    @OnItemClick(R.id.listView)
    public void itemOnClick(AdapterView<?> parent, View view, int position, long id){
        final BleDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mBle.isScanning()) {
            mBle.stopScan();
        }
        if (device.isConnected()) {
            mBle.disconnect(device);
        } else if (!device.isConnectting()) {
            //扫描到设备时   务必用该方式连接(是上层逻辑问题， 否则点击列表  虽然能够连接上，但设备列表的状态不会发生改变)
            mBle.connect(device, connectCallback);
            //此方式只是针对不进行扫描连接（如上，若通过该方式进行扫描列表的连接  列表状态不会发生改变）
//            mBle.connect(device.getBleAddress(), connectCallback);
        }
    }

    @Override
    protected void onInitView() {
        initView();
        path = Environment.getExternalStorageDirectory()
                + "/aceDownload/";
        //1、请求蓝牙相关权限
        requestPermission();
    }

    //初始化蓝牙
    private void initBle() {
        mBle = Ble.getInstance();
        Ble.Options options = new Ble.Options();
        options.logBleExceptions = true;//设置是否输出打印蓝牙日志
        options.throwBleException = true;//设置是否抛出蓝牙异常
        options.autoConnect = false;//设置是否自动连接
        options.scanPeriod = 12 * 1000;//设置扫描时长
        options.connectTimeout = 10 * 1000;//设置连接超时时长
        options.uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb");//设置主服务的uuid
        //options.uuid_services_extra = new UUID[]{UUID.fromString("0000180f-0000-1000-8000-00805f9b34fb")};//添加额外的服务（如电量服务，心跳服务等）
        options.uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600");//设置可写特征的uuid
        //options.uuid_read_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129601");//设置可读特征的uuid
        //ota相关 修改为你们自己的
       /* options.uuid_ota_service = UUID.fromString("0000fee8-0000-1000-8000-00805f9b34fb");
        options.uuid_ota_notify_cha = UUID.fromString("003784cf-f7e3-55b4-6c4c-9fd140100a16");
        options.uuid_ota_write_cha = UUID.fromString("013784cf-f7e3-55b4-6c4c-9fd140100a16");*/
        mBle.init(getApplicationContext(), options);
        //3、检查蓝牙是否支持及打开
        checkBluetoothStatus();
    }

    //检查蓝牙是否支持及打开
    private void checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!mBle.isSupportBle(this)) {
            ToastUtil.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!mBle.isBleEnable()) {
            //4、若未打开，则请求打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        }else {
            //5、若已打开，则进行扫描
            mBle.startScan(scanCallback);
        }
    }

    //请求权限
    private void requestPermission(){
        requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN,
                        Manifest.permission.ACCESS_COARSE_LOCATION},
                "请求蓝牙相关权限", new GrantedResult() {
                    @Override
                    public void onResult(boolean granted) {
                        if (granted) {
                            //2、初始化蓝牙
                            initBle();
                        } else {
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void initLinsenter() {}

    private void initView() {
        setTitle("Ble界面");
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
    }

    /*发送数据*/
    public void sendData(BleDevice device) {
        CommandBean commandBean = new CommandBean();
        AppProtocol.sendCarCmdCommand(device, commandBean.setCarCommand(80, 1));
//        AppProtocol.sendCarMoveCommand(device, commandBean.setOrderCommand(2,1,null));
//        AppProtocol.sendCarMscCommand(device, commandBean.setMscCommand(C.Command.TF_MUSIC_TYPE, 1, (short) 121));
//        AppProtocol.sendMusicVolume(device, commandBean.setVolumeCommand(C.Command.TF_MUSIC_TYPE, 10));
    }

    /*主动读取数据*/
    public void read(BleDevice device) {
        boolean result = mBle.read(device, new BleReadCallback<BleDevice>() {
            @Override
            public void onReadSuccess(BluetoothGattCharacteristic characteristic) {
                super.onReadSuccess(characteristic);
                byte[] data = characteristic.getValue();
                Log.w(TAG, "onReadSuccess: " + Arrays.toString(data));
            }
        });
        if (!result) {
            Log.d(TAG, "读取数据失败!");
        }
    }

    //扫描的回调
    BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            synchronized (mBle.getLocker()) {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            Log.e(TAG, "onStop: ");
        }
    };

    /*连接的回调*/
    private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(final BleDevice device) {
            if (device.isConnected()) {
                 /*连接成功后，设置通知*/
                mBle.startNotify(device, bleNotiftCallback);
            }
            Log.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
            setConnectedNum();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            ToastUtil.showToast("连接异常，异常状态码:" + errorCode);
        }
    };

    /*设置通知的回调*/
    private BleNotiftCallback<BleDevice> bleNotiftCallback =  new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            Log.e(TAG, "onChanged==uuid:" + uuid.toString());
            Log.e(TAG, "onChanged==address:"+ device.getBleAddress());
            Log.e(TAG, "onChanged==data:" + Arrays.toString(characteristic.getValue()));
        }
    };

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
        if (!SPUtils.get(BleActivity.this, C.SP.IS_FIRST_RUN, true)) {
            return;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                // 获取SD卡路径
                File file = new File(path);
                // 如果SD卡目录不存在创建
                if (!file.exists()) {
                    file.mkdir();
                }

                final File newFile = new File(path + C.Constance.OTA_NEW_PATH);
                final File oldFile = new File(path + C.Constance.OTA_OLD_PATH);
                try {
                    FileUtils.copyBigDataToSD(BleActivity.this, C.Constance.OTA_NEW_PATH, newFile.getAbsolutePath());
                    FileUtils.copyBigDataToSD(BleActivity.this, C.Constance.OTA_OLD_PATH, oldFile.getAbsolutePath());
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //设置程序非第一次进入
                SPUtils.put(BleActivity.this, C.SP.IS_FIRST_RUN, false);
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
                L.e(this, "点击了扫描按钮");
                reScan();
                break;
            case R.id.menu_stop:
                L.e(this, "点击了停止扫描按钮");
                if (mBle != null) {
                    mBle.stopScan();
                }
                break;
            case R.id.menu_connect_all:
                L.e(this, "点击了连接全部设备按钮");
                if (mBle != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mBle.connect(device, connectCallback);
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                L.e(this, "点击了断开全部设备按钮");
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
            case R.id.menu_share:
                Intent textIntent = new Intent(Intent.ACTION_SEND);
                textIntent.setType("text/plain");
                textIntent.putExtra(Intent.EXTRA_TEXT, "这是一段分享的文字");
                startActivity(Intent.createChooser(textIntent, "分享"));
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
        } else if(requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK){
            //6、若打开，则进行扫描
            mBle.startScan(scanCallback);
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

    //inputstream转byte[]
    public static byte[] toByteArray(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int n = 0;
        while (-1 != (n = input.read(buffer))) {
            output.write(buffer, 0, n);
        }
        return output.toByteArray();
    }

}
