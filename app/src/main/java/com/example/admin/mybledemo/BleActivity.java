package com.example.admin.mybledemo;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import com.example.admin.mybledemo.aop.CheckConnect;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleStatusCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.callback.BleConnectCallback;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.queue.RequestTask;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;
import cn.com.heaton.blelibrary.ble.utils.CrcUtils;
import cn.com.heaton.blelibrary.ota.OtaManager;
import cn.com.superLei.aoparms.annotation.Permission;
import cn.com.superLei.aoparms.annotation.PermissionDenied;
import cn.com.superLei.aoparms.annotation.PermissionNoAskDenied;
import cn.com.superLei.aoparms.annotation.Retry;
import cn.com.superLei.aoparms.common.permission.AopPermissionUtils;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
public class BleActivity extends AppCompatActivity implements View.OnClickListener {
    private String TAG = BleActivity.class.getSimpleName();
    public static final int REQUEST_PERMISSION_LOCATION = 2;
    public static final int REQUEST_PERMISSION_WRITE = 3;
    private Button readRssi, sendData, updateOta, requestMtu, sendEntityData, sendAutoEntityData, writeQueue;
    private ListView listView;
    private DeviceAdapter adapter;
    private List<BleDevice> bleDevices;
    private Ble<BleDevice> ble = Ble.getInstance();
    private String path = Environment.getExternalStorageDirectory() + "/AndroidBleOTA/";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ble);
        initView();
        initLinsenter();
        initAdapter();
        //1、请求蓝牙相关权限
        requestPermission();
    }

    private void initAdapter() {
        bleDevices = new ArrayList<>();
        adapter = new DeviceAdapter(this, bleDevices);
        listView.setAdapter(adapter);
    }

    private void initView() {
        listView = findViewById(R.id.listView);
        readRssi = findViewById(R.id.readRssi);
        sendData = findViewById(R.id.sendData);
        updateOta = findViewById(R.id.updateOta);
        requestMtu = findViewById(R.id.requestMtu);
        sendEntityData = findViewById(R.id.sendEntityData);
        sendAutoEntityData = findViewById(R.id.sendAutoEntityData);
        writeQueue = findViewById(R.id.writeQueue);
    }

    private void initLinsenter() {
        readRssi.setOnClickListener(this);
        sendData.setOnClickListener(this);
        updateOta.setOnClickListener(this);
        requestMtu.setOnClickListener(this);
        sendEntityData.setOnClickListener(this);
        sendAutoEntityData.setOnClickListener(this);
        writeQueue.setOnClickListener(this);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BleDevice device = adapter.getItem(position);
                if (device == null) return;
                if (ble.isScanning()) {
                    ble.stopScan();
                }
                if (device.isConnected()) {
                    ble.disconnect(device);
                } else if (device.isConnectting()) {
                    ble.cancelConnectting(device);
                } else if (device.isDisconnected()) {
                    //扫描到设备时   务必用该方式连接(是上层逻辑问题， 否则点击列表  虽然能够连接上，但设备列表的状态不会发生改变)
                    ble.connect(device, connectCallback);
                }
            }
        });
    }

    //请求权限
    @Permission(value = {Manifest.permission.ACCESS_COARSE_LOCATION},
            requestCode = REQUEST_PERMISSION_LOCATION,
            rationale = "需要蓝牙相关权限")
    public void requestPermission() {
        checkBluetoothStatus();
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

    /**
     * 监听蓝牙开关状态
     */
    private void initBleStatus() {
        ble.setBleStatusCallback(new BleStatusCallback() {
            @Override
            public void onBluetoothStatusChanged(boolean isOn) {
                BleLog.i(TAG, "onBluetoothStatusOn: 蓝牙是否打开>>>>:" + isOn);
            }
        });
    }

    /**
     * 检查蓝牙是否支持及打开
     */
    private void checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!ble.isSupportBle(this)) {
            Utils.showToast(R.string.ble_not_supported);
            finish();
        }
        if (!ble.isBleEnable()) {
            //4、若未打开，则请求打开蓝牙
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT);
        } else {
            //5、若已打开，则进行扫描
            ble.startScan(scanCallback);
        }
    }

    @CheckConnect //检查是否连接
    @Override
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
            case R.id.writeQueue:
                writeQueue();
                break;
            default:
                break;
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
                    ble.cancelWriteEntity();
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

    @CheckConnect //检查是否连接
    private void writeQueue() {
        String address = ble.getConnetedDevices().get(0).getBleAddress();
        for (int i = 0; i < 10; i++) {
//            ble.writeQueueDelay(500, RequestTask.newWriteTask(address, "hello android".getBytes()));
            ble.writeQueue(RequestTask.newWriteTask(address, "hello android".getBytes()));
        }
    }

    /**
     * 分包发送数据
     *
     * @param autoWriteMode 是否分包自动发送数据(entityData中的delay无效)
     */
    private void sendEntityData(boolean autoWriteMode) {
        EntityData entityData = getEntityData(autoWriteMode);
        if (entityData == null) return;
        showProgress();
        ble.writeEntity(entityData, writeEntityCallback);
    }

    private BleWriteEntityCallback<BleDevice> writeEntityCallback = new BleWriteEntityCallback<BleDevice>() {
        @Override
        public void onWriteSuccess() {
            BleLog.d("writeEntity", "onWriteSuccess");
            hideProgress();
            toToast("发送成功");
        }

        @Override
        public void onWriteFailed() {
            BleLog.d("writeEntity", "onWriteFailed");
            hideProgress();
            toToast("发送失败");
        }

        @Override
        public void onWriteProgress(double progress) {
            Log.d("writeEntity", "当前发送进度: " + progress);
            setDialogProgress((int) (progress * 100));
        }

        @Override
        public void onWriteCancel() {
            Log.d(TAG, "onWriteCancel: ");
            hideProgress();
            toToast("发送取消");
        }
    };

    private void toToast(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Utils.showToast(msg);
            }
        });
    }

    private EntityData getEntityData(boolean autoWriteMode) {
        InputStream inputStream = null;
        try {
            inputStream = getAssets().open("WhiteChristmas.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (inputStream == null) {
            Utils.showToast("不能发现文件!");
            return null;
        }
        byte[] data = ByteUtils.stream2Bytes(inputStream);
        Log.e(TAG, "data length: " + data.length);
        BleDevice device = ble.getConnetedDevices().get(0);
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
            ble.setMTU(ble.getConnetedDevices().get(0).getBleAddress(), 96, new BleMtuCallback<BleDevice>() {
                @Override
                public void onMtuChanged(BleDevice device, int mtu, int status) {
                    super.onMtuChanged(device, mtu, status);
                    Utils.showToast("最大支持MTU：" + mtu);
                }
            });
        } else {
            Utils.showToast("设备不支持MTU");
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
        Utils.copyOtaFile(BleActivity.this, path);
        SystemClock.sleep(200);
        File file = new File(path + Constant.Constance.OTA_FILE_PATH);
        OtaManager mOtaManager = new OtaManager(BleActivity.this);
        boolean result = mOtaManager.startOtaUpdate(file, ble.getConnetedDevices().get(0), ble);
        BleLog.e("OTA升级结果:", result + "");
    }

    /**
     * 发送数据
     */
    @Retry(count = 3, delay = 100, asyn = true)
    private void sendData() {
        byte[] data = new byte[20];
        data[0] = 0x01;
        data[1] = (byte) 0xfe;
        data[2] = 0x08;
        data[3] = 0x35;
        data[4] = (byte) 0xf1;
        data[5] = (byte) CrcUtils.CRC8.CRC8(data, 0, data.length);
        Log.e(TAG, "sendData: "+ByteUtils.toHexString(data));
//        return ble.write(list.get(0), "Hello Android!".getBytes(), new BleWriteCallback<BleDevice>() {
        ble.write(ble.getConnetedDevices().get(0), data, new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                Log.e(TAG, "onWriteSuccess: ");
            }

            @Override
            public void onWiteFailed(BleDevice device, String message) {
                Log.e(TAG, "onWiteFailed: " + message);
            }
        });
    }

    /**
     * 主动读取数据
     *
     * @param device 设备对象
     */
    @Retry(count = 3, delay = 100, asyn = true)
    public boolean read(BleDevice device) {
        return ble.read(device, new BleReadCallback<BleDevice>() {
            @Override
            public void onReadSuccess(BleDevice bleDevice, BluetoothGattCharacteristic characteristic) {
                byte[] data = characteristic.getValue();
                BleLog.w(TAG, "onReadSuccess: " + Arrays.toString(data));
            }
        });
    }

    /**
     * 读取远程rssi
     */
    private void readRssi() {
        ble.readRssi(ble.getConnetedDevices().get(0), new BleReadRssiCallback<BleDevice>() {
            @Override
            public void onReadRssiSuccess(BleDevice bleDevice, int rssi) {
                BleLog.e(TAG, "onReadRssiSuccess: " + rssi);
                Utils.showToast("读取远程RSSI成功：" + rssi);
            }
        });
    }

    /**
     * 重新扫描
     */
    private void reScan() {
        if (ble != null && !ble.isScanning()) {
            bleDevices.clear();
            bleDevices.addAll(ble.getConnetedDevices());
            adapter.notifyDataSetChanged();
            ble.startScan(scanCallback);
        }
    }

    /**
     * 扫描的回调
     */
    private BleScanCallback<BleDevice> scanCallback = new BleScanCallback<BleDevice>() {
        @Override
        public void onLeScan(final BleDevice device, int rssi, byte[] scanRecord) {
            BleLog.i(TAG, "onLeScan: " + device.getBleName());
            if (TextUtils.isEmpty(device.getBleName())) return;
            synchronized (ble.getLocker()) {
                for (BleDevice bleDevice: bleDevices) {
                    if (TextUtils.equals(bleDevice.getBleAddress(), device.getBleAddress())){
                        return;
                    }
                }
                bleDevices.add(device);
                adapter.notifyDataSetChanged();
            }
        }

        /*@Override
        public void onStop() {
            super.onStop();
            BleLog.e(TAG, "onStop: ");
        }*/

        /*@Override
        public void onScanFailed(int errorCode) {
            BleLog.e(TAG, "onScanFailed: "+errorCode);
        }*/

        /*@Override
        public void onParsedData(BleDevice device, ScanRecord scanRecord) {
            super.onParsedData(device, scanRecord);
            byte[] data = scanRecord.getManufacturerSpecificData(65520);//参数为厂商id
            if (data != null) {
                Log.e(TAG, "onParsedData: " + ByteUtils.bytes2HexStrWithSpace(data));
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
            adapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Utils.showToast("连接异常，异常状态码:" + errorCode);
            hideProgress();
        }

        @Override
        public void onConnectTimeOut(BleDevice device) {
            super.onConnectTimeOut(device);
            Log.e(TAG, "onConnectTimeOut: " + device.getBleAddress());
            Utils.showToast("连接超时:" + device.getBleName());
        }

        @Override
        public void onConnectCancel(BleDevice device) {
            super.onConnectCancel(device);
            Log.e(TAG, "onConnectCancel: " + device.getBleName());
        }

        @Override
        public void onReady(BleDevice device) {
            super.onReady(device);
            /*连接成功后，设置通知*/
            ble.startNotify(device, bleNotiftCallback);
        }
    };

    /**
     * 设置通知的回调
     */
    private BleNotiftCallback<BleDevice> bleNotiftCallback = new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
            UUID uuid = characteristic.getUuid();
            BleLog.e(TAG, "onChanged==uuid:" + uuid.toString());
            BleLog.e(TAG, "onChanged==address:" + device.getBleAddress());
            BleLog.e(TAG, "onChanged==data:" + Arrays.toString(characteristic.getValue()));
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Utils.showToast(String.format("收到设备通知数据: %s", ByteUtils.toHexString(characteristic.getValue())));
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
                if (ble != null) {
                    ble.stopScan();
                }
                break;
            case R.id.menu_connect_all:
                ble.connects(bleDevices, connectCallback);
                break;
            case R.id.menu_disconnect_all:
                if (ble != null) {
                    List<BleDevice> list = ble.getConnetedDevices();
                    BleLog.e(TAG, "onOptionsItemSelected:>>>> " + list.size());
                    for (int i = 0; i < list.size(); i++) {
                        ble.disconnect(list.get(i));
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
            ble.startScan(scanCallback);
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
