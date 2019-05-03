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
import com.example.admin.mybledemo.aop.SingleClick;
import com.example.admin.mybledemo.command.AppProtocol;
import com.example.admin.mybledemo.command.CommandBean;
import com.example.admin.mybledemo.utils.ByteUtils;
import com.example.admin.mybledemo.utils.FileUtils;
import com.example.admin.mybledemo.utils.ToastUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ota.OtaManager;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@ContentView(R.layout.activity_ble)
public class BleActivity extends BaseActivity {
    private String TAG = BleActivity.class.getSimpleName();
    @ViewInit(R.id.listView)
    ListView mListView;
    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private String path = Environment.getExternalStorageDirectory()
            + "/aceDownload/";

    @Override
    protected void onInitView() {
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        //1、请求蓝牙相关权限
        requestPermission();
    }

    //初始化蓝牙
    private void initBle() {
        mBle = Ble.options()
                .setLogBleExceptions(true)//设置是否输出打印蓝牙日志
                .setThrowBleException(true)//设置是否抛出蓝牙异常
                .setAutoConnect(false)//设置是否自动连接
                .setConnectFailedRetryCount(3)
                .setConnectTimeout(10 * 1000)//设置连接超时时长
                .setScanPeriod(12 * 1000)//设置扫描时长
                .setUuid_service(UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb"))//设置主服务的uuid
                .setUuid_write_cha(UUID.fromString("d44bc439-abfd-45a2-b575-925416129600"))//设置可写特征的uuid
                .create(getApplicationContext());
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
        } else {
            //5、若已打开，则进行扫描
            mBle.startScan(scanCallback);
        }
    }

    //请求权限
    private void requestPermission() {
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

    @SingleClick //过滤重复点击
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
    @OnClick({R.id.test, R.id.readRssi, R.id.sendData, R.id.updateOta, R.id.requestMtu, R.id.sendEntityData, R.id.cancelEntity})
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.test:
                toTest();
                break;
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
                try {
                    showProgress();
                    sendEntityData();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;
            case R.id.cancelEntity:
                cancelEntity();
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
    private void showProgress(){
        if (dialog == null){
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
                    cancelEntity();
                }
            });
        }
        dialog.show();
    }

    private void setDialogProgress(int progress){
        Log.e(TAG, "setDialogProgress: "+progress);
        if (dialog != null){
            dialog.setProgress(progress);
        }
    }

    private void hideProgress(){
        if (dialog != null){
            dialog.dismiss();
        }
    }

    /**
     * 发送大数据量的包
     */
    private void sendEntityData() throws IOException {
        byte[] data = ByteUtils.toByteArray(getAssets().open("WhiteChristmas.bin"));
        Log.e(TAG, "sendEntityData: "+data.length);
        mBle.writeEntity(mBle.getConnetedDevices().get(0), data, 20, 50, new BleWriteEntityCallback<BleDevice>() {
            @Override
            public void onWriteSuccess() {
                L.e("writeEntity", "onWriteSuccess");
                hideProgress();
            }

            @Override
            public void onWriteFailed() {
                L.e("writeEntity", "onWriteFailed");
                hideProgress();
            }

            @Override
            public void onWriteProgress(double progress) {
                Log.e("writeEntity", "当前发送进度: "+progress);
                setDialogProgress((int) (progress * 100));
            }

            @Override
            public void onWriteCancel() {
                Log.e(TAG, "onWriteCancel: ");
                hideProgress();
            }
        });
    }

    /**
     * 取消发送大数据包
     */
    private void cancelEntity() {
        mBle.cancelWriteEntity();
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
    private void updateOta() {
        //此处为了方便把OTA升级文件直接放到assets文件夹下，拷贝到/aceDownload/文件夹中  以便使用
        requestPermission(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE},
                "读写SD卡相关权限", new GrantedResult() {
                    @Override
                    public void onResult(boolean granted) {
                        if (granted) {
                            FileUtils.copyAssets2SD(BleActivity.this, path);
                        } else {
                            ToastUtil.showToast("读写SD卡权限被拒绝,将会影响OTA升级功能哦!");
                        }
                    }
                });
        File file = new File(path + C.Constance.OTA_NEW_PATH);
        OtaManager mOtaManager = new OtaManager(BleActivity.this);
        boolean result = mOtaManager.startOtaUpdate(file, (BleDevice) mBle.getConnetedDevices().get(0), mBle);
        L.e("OTA升级结果:", result + "");
    }

    /**
     * 发送数据
     */
    private void sendData() {
        List<BleDevice> list = Ble.getInstance().getConnetedDevices();
        synchronized (mBle.getLocker()) {
            for (BleDevice device : list) {
                CommandBean commandBean = new CommandBean();
                AppProtocol.sendCarMoveCommand(device, commandBean.setCarCommand(80, 1));
//                AppProtocol.sendCarMoveCommand(device, commandBean.setOrderCommand(2, 1, null));
//                AppProtocol.sendCarMscCommand(device, commandBean.setMscCommand(C.Command.TF_MUSIC_TYPE, 1, (short) 121));
//                AppProtocol.sendMusicVolume(device, commandBean.setVolumeCommand(C.Command.TF_MUSIC_TYPE, 10));
            }
        }
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

    private void toTest() {
        if (mBle.isScanning()) {
            mBle.stopScan();
        }
        startActivity(new Intent(BleActivity.this, TestActivity.class));
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
     * 主动读取数据
     * @param device 设备对象
     */
    public void read(BleDevice device) {
        boolean result = mBle.read(device, new BleReadCallback<BleDevice>() {
            @Override
            public void onReadSuccess(BluetoothGattCharacteristic characteristic) {
                super.onReadSuccess(characteristic);
                byte[] data = characteristic.getValue();
                L.w(TAG, "onReadSuccess: " + Arrays.toString(data));
            }
        });
        if (!result) {
            L.d(TAG, "读取数据失败!");
        }
    }

    /**
     * 扫描的回调
     */
    BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            Log.e(TAG, "onLeScan: "+device.getBleAddress());
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

        @Override
        public void onParsedData(BleDevice device, ScanRecord scanRecord) {
            super.onParsedData(device, scanRecord);
            byte[] data = scanRecord.getManufacturerSpecificData(65535);//参数为厂商id
            if (data != null){
                Log.e(TAG, "onParsedData: "+ ByteUtils.BinaryToHexString(data));
            }
        }
    };

    /**
     * 连接的回调
     */
    private BleConnectCallback<BleDevice> connectCallback = new BleConnectCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            Log.e(TAG, "onConnectionChanged: "+device.getConnectionState());
            Log.e(TAG, "onConnectionChanged: current thread:"+Thread.currentThread().getName());
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
        }

        @Override
        public void onConnectTimeOut(BleDevice device) {
            super.onConnectTimeOut(device);
            Log.e(TAG, "onConnectTimeOut: "+device.getBleAddress());
            ToastUtil.showToast("连接超时:"+device.getBleName());
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
