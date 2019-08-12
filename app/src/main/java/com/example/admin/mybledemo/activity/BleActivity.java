package com.example.admin.mybledemo.activity;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import com.example.admin.mybledemo.C;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.adapter.LeDeviceListAdapter;
import com.example.admin.mybledemo.annotation.ContentView;
import com.example.admin.mybledemo.annotation.OnClick;
import com.example.admin.mybledemo.annotation.OnItemClick;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.example.admin.mybledemo.aop.CheckConnect;
import com.example.admin.mybledemo.utils.ByteUtils;
import com.example.admin.mybledemo.utils.FileUtils;
import com.example.admin.mybledemo.utils.RetryUtils;
import com.example.admin.mybledemo.utils.ToastUtil;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ota.OtaManager;
import cn.com.superLei.aoparms.annotation.Permission;
import cn.com.superLei.aoparms.annotation.PermissionDenied;
import cn.com.superLei.aoparms.annotation.PermissionNoAskDenied;
import cn.com.superLei.aoparms.annotation.Retry;
import cn.com.superLei.aoparms.annotation.SingleClick;
import cn.com.superLei.aoparms.common.permission.AopPermissionUtils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@ContentView(R.layout.activity_ble)
public class BleActivity extends BaseActivity {
    private String TAG = BleActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_PERMISSION_WRITE = 3;
    @ViewInit(R.id.listView)
    ListView mListView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private String path = Environment.getExternalStorageDirectory() + "/AndroidBleOTA/";

    @Override
    protected void onInitView() {
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        //1、请求蓝牙相关权限
        requestPermission();
    }

    //请求权限
    @Permission(value = {Manifest.permission.ACCESS_COARSE_LOCATION},
            requestCode = REQUEST_PERMISSION_LOCATION,
            rationale = "需要蓝牙相关权限")
    public void requestPermission() {
        initBle();
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


    //初始化蓝牙
    private void initBle() {
        mBle = Ble.options()
                .setLogBleExceptions(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setAutoConnect(false)//设置是否自动连接
                .setFilterScan(true)//设置是否过滤扫描到的设备
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(12 * 1000)//设置扫描时长
                .setUuidService(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))//设置主服务的uuid
                .setUuidWriteCha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))//设置可写特征的uuid
//                .setUuidOtaService(UUID.fromString("0000fd00-0000-1000-8000-00805f9b34fb"))
//                .setUuidOtaNotifyCha(UUID.fromString("0000fd02-0000-1000-8000-00805f9b34fb"))
//                .setUuidOtaWriteCha(UUID.fromString("0000fd01-0000-1000-8000-00805f9b34fb"))
                .create(getApplicationContext());
        //3、检查蓝牙是否支持及打开
        checkBluetoothStatus();
        //监听蓝牙开关状态
        initBleStatus();
    }

    private void initBleStatus() {
        mBle.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusOn() {
                L.i(TAG, "onBluetoothStatusOn: 蓝牙开启>>>>>");
            }

            @Override
            public void onBluetoothStatusOff() {
                L.i(TAG, "onBluetoothStatusOff: 蓝牙关闭>>>>>");
            }
        });
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
        } else {
            //5、若已打开，则进行扫描
            mBle.startScan(scanCallback);
        }
    }

    //过滤重复点击
    @SingleClick
    @OnClick({R.id.startAdvertise, R.id.stopAdvertise})
    public void onAdvertiseClick(View view) {
        switch (view.getId()) {
            case R.id.startAdvertise:
                byte[] payload = new byte[16];
                payload[0] = 0x01;
                mBle.startAdvertising(payload);
                break;
            case R.id.stopAdvertise:
                mBle.stopAdvertising();
                break;
            default:
                break;
        }
    }

    @SingleClick //过滤重复点击
    @CheckConnect //检查是否连接
    @OnClick({R.id.readRssi, R.id.sendData, R.id.updateOta, R.id.requestMtu, R.id.sendEntityData, R.id.sendAutoEntityData})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.readRssi:
                readRssi();
                break;
            case R.id.sendData:
                sendData();
                break;
            case R.id.updateOta:
                updateOta();
                break;
            case R.id.requestMtu:
                requestMtu();
                break;
            case R.id.sendEntityData:
                sendEntityData(false);
                break;
            case R.id.sendAutoEntityData:
                sendEntityData(true);
                break;
            default:
                break;
        }
    }

    @OnItemClick(R.id.listView)
    public void itemOnClick(AdapterView<?> parent, View view, int position, long id) {
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

    ProgressDialog dialog;

    private void showProgress() {
        if (dialog == null) {
            dialog = new ProgressDialog(this);
            dialog.setCancelable(false);
            dialog.setCanceledOnTouchOutside(false);// 设置在点击Dialog外是否取消Dialog进度条
            dialog.setTitle("发送大数据文件");
            dialog.setIcon(R.mipmap.ic_launcher);
            dialog.setMessage("Data is sending, please wait...");
            dialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
            dialog.setMax(100);
            dialog.setIndeterminate(false);
            dialog.setButton(DialogInterface.BUTTON_NEGATIVE, "取消", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    mBle.cancelWriteEntity();
                }
            });
        }
        dialog.show();
    }

    private void setDialogProgress(int progress) {
        Log.e(TAG, "setDialogProgress: " + progress);
        if (dialog != null) {
            dialog.setProgress(progress);
        }
    }

    private void hideProgress() {
        if (dialog != null) {
            dialog.cancel();
            dialog = null;
            Log.e(TAG, "hideProgress: ");
        }
    }

    /**
     * 分包发送数据
     * @param autoWriteMode 是否分包自动发送数据(entityData中的delay无效)
     */
    private void sendEntityData(boolean autoWriteMode) {
        EntityData entityData = getEntityData(autoWriteMode);
        if (entityData == null)return;
        showProgress();
        mBle.writeEntity(entityData, writeEntityCallback);
    }

    private BleWriteEntityCallback<BleDevice> writeEntityCallback = new BleWriteEntityCallback<BleDevice>() {
        @Override
        public void onWriteSuccess() {
            L.e("writeEntity", "onWriteSuccess");
            hideProgress();
            toToast("发送成功");
        }

        @Override
        public void onWriteFailed() {
            L.e("writeEntity", "onWriteFailed");
            hideProgress();
            toToast("发送失败");
        }

        @Override
        public void onWriteProgress(double progress) {
            Log.e("writeEntity", "当前发送进度: " + progress);
            setDialogProgress((int) (progress * 100));
        }

        @Override
        public void onWriteCancel() {
            Log.e(TAG, "onWriteCancel: ");
            hideProgress();
            toToast("发送取消");
        }
    };

    private void toToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                ToastUtil.showToast(msg);
            }
        });
    }

    private EntityData getEntityData(boolean autoWriteMode){
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("WhiteChristmas.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputStream == null){
            ToastUtil.showToast("不能发现文件!");
            return null;
        }
        byte[] data = ByteUtils.toByteArray(inputStream);
        Log.e(TAG, "data length: " + data.length);
        BleDevice device = mBle.getConnetedDevices().get(0);
        return new EntityData.Builder()
                .setAutoWriteMode(autoWriteMode)
                .setAddress(device.getBleAddress())
                .setData(data)
                .setPackLength(20)
                .setDelay(50L)
                .build();
    }
    /**
     * 设置请求MTU
     */
    private void requestMtu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
            mBle.setMTU(mBle.getConnetedDevices().get(0).getBleAddress(), 96, new BleMtuCallback<BleDevice>() {
                @Override
                public void onMtuChanged(BleDevice device, int mtu, int status) {
                    super.onMtuChanged(device, mtu, status);
                    ToastUtil.showToast("最大支持MTU：" + mtu);
                }
            });
        } else {
            ToastUtil.showToast("设备不支持MTU");
        }
    }

    /**
     * OTA升级
     */
    @Permission(value = {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE},
            requestCode = REQUEST_PERMISSION_WRITE,
            rationale = "读写SD卡权限被拒绝,将会影响OTA升级功能哦!")
    public void updateOta() {
        //此处为了方便把OTA升级文件直接放到assets文件夹下，拷贝到/aceDownload/文件夹中  以便使用
        FileUtils.copyAssets2SD(BleActivity.this, path);
        SystemClock.sleep(200);
        File file = new File(path + C.Constance.OTA_NEW_PATH);
        OtaManager mOtaManager = new OtaManager(BleActivity.this);
        boolean result = mOtaManager.startOtaUpdate(file, mBle.getConnetedDevices().get(0), mBle);
        L.e("OTA升级结果:", result + "");
    }

    /**
     * 发送数据
     */
    @Retry(count = 3, delay = 100, asyn = true)
    private boolean sendData() {
        Log.e(TAG, "sendData: >>>>");
        List<BleDevice> list = mBle.getConnetedDevices();
        return mBle.write(list.get(0), "Hello Android!".getBytes(), null);
    }

    /**
     * 主动读取数据
     *
     * @param device 设备对象
     */
    @Retry(count = 3, delay = 100, asyn = true)
    public boolean read(BleDevice device) {
        return mBle.read(device, new BleReadCallback<BleDevice>() {
            @Override
            public void onReadSuccess(BluetoothGattCharacteristic characteristic) {
                super.onReadSuccess(characteristic);
                byte[] data = characteristic.getValue();
                L.w(TAG, "onReadSuccess: " + Arrays.toString(data));
            }
        });
    }

    /**
     * 读取远程rssi
     */
    private void readRssi() {
        mBle.readRssi(mBle.getConnetedDevices().get(0), new BleReadRssiCallback<BleDevice>() {
            @Override
            public void onReadRssiSuccess(int rssi) {
                super.onReadRssiSuccess(rssi);
                L.e(TAG, "onReadRssiSuccess: " + rssi);
                ToastUtil.showToast("读取远程RSSI成功：" + rssi);
            }
        });
    }

    /**
     * 重新扫描
     */
    private void reScan() {
        if (mBle != null && !mBle.isScanning()) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
            mBle.startScan(scanCallback);
        }
    }

    /**
     * 扫描的回调
     */
    private BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG, "onLeScan: " + device.getBleName());
            if (TextUtils.isEmpty(device.getBleName())) return;
            synchronized (mBle.getLocker()) {
                mLeDeviceListAdapter.addDevice(device);
                mLeDeviceListAdapter.notifyDataSetChanged();
            }
        }

        @Override
        public void onStop() {
            super.onStop();
            L.e(TAG, "onStop: ");
        }

        /*@Override
        public void onParsedData(BleDevice device, ScanRecord scanRecord) {
            super.onParsedData(device, scanRecord);
            byte[] data = scanRecord.getManufacturerSpecificData(65520);//参数为厂商id
            if (data != null) {
                Log.e(TAG, "onParsedData: " + ByteUtils.BinaryToHexString(data));
            }
        }*/
    };

    /**
     * 连接的回调
     */
    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: " + device.getConnectionState());
            if (device.isConnected()) {
                /*连接成功后，设置通知*/
                mBle.startNotify(device, bleNotiftCallback);
            }
            L.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            ToastUtil.showToast("连接异常，异常状态码:" + errorCode);
            hideProgress();
        }

        @Override
        public void onConnectTimeOut(BleDevice device) {
            super.onConnectTimeOut(device);
            Log.e(TAG, "onConnectTimeOut: " + device.getBleAddress());
            ToastUtil.showToast("连接超时:" + device.getBleName());
        }
    };

    /**
     * 设置通知的回调
     */
    private BleNotiftCallback<BleDevice> bleNotiftCallback = new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            L.e(TAG, "onChanged==uuid:" + uuid.toString());
            L.e(TAG, "onChanged==address:" + device.getBleAddress());
            L.e(TAG, "onChanged==data:" + Arrays.toString(characteristic.getValue()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ToastUtil.showToast(String.format("收到设备通知数据: %s", ByteUtils.BinaryToHexString(characteristic.getValue())));
                }
            });
        }
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                reScan();
                break;
            case R.id.menu_stop:
                if (mBle != null) {
                    mBle.stopScan();
                }
                break;
            case R.id.menu_connect_all:
                if (mBle != null) {
                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                        BleDevice device = mLeDeviceListAdapter.getDevice(i);
                        mBle.connect(device, connectCallback);
                    }
                }
                break;
            case R.id.menu_disconnect_all:
                if (mBle != null) {
                    ArrayList<BleDevice> list = mBle.getConnetedDevices();
                    L.e(TAG, "onOptionsItemSelected:>>>> " + list.size());
                    for (int i = 0; i < list.size(); i++) {
                        mBle.disconnect(list.get(i));
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

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
        } else if (requestCode == Ble.REQUEST_ENABLE_BT && resultCode == Activity.RESULT_OK) {
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

}
