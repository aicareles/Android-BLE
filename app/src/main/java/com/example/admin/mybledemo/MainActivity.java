package com.example.admin.mybledemo;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;

/**
 * Activity for scanning and displaying available Bluetooth LE devices.
 */
@SuppressLint("NewApi")
public class MainActivity extends BaseActivity {
    private String TAG = MainActivity.class.getSimpleName();

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private BluetoothAdapter mBluetoothAdapter;
    private boolean mScanning;
    private ListView mListView;
    private TextView mConnectedNum;
    private Button send;
    private BluetoothLeService mBluetoothLeService;
    private String currentAddress;

    private static final int REQUEST_ENABLE_BT = 1;
    // stop scan after 10 second
    public static final long SCAN_PERIOD = 10000;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case 1:
                    mBluetoothLeService.setOnBleLisenter(new BleLisenter() {
                        @Override
                        public void onStart() {
                            super.onStart();
                            mScanning = true;
                        }

                        @Override
                        public void onStop() {
                            super.onStop();
                            mScanning = false;
                        }

                        @Override
                        public void onLeScan(BluetoothDevice device, int rssi, byte[] scanRecord) {
                            Log.e(TAG, "onLeScan");
                            BleDevice bleDevice = new BleDevice(device);
                            mLeDeviceListAdapter.addDevice(bleDevice);
                            mLeDeviceListAdapter.notifyDataSetChanged();
                        }

                        @Override
                        public void onConnectionChanged(final BluetoothGatt gatt, final BleDevice device) {
                            Log.e(TAG, "onConnectionChanged");
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    setConnectedNum();
                                    for (int i = 0; i < mLeDeviceListAdapter.getCount(); i++) {
                                        if (device.getBleAddress().equals(mLeDeviceListAdapter.getDevice(i).getBleAddress())) {
                                            if (device.isConnected()) {
                                                mLeDeviceListAdapter.getDevice(i).setConnected(true);
                                                Toast.makeText(MainActivity.this, "连接成功", Toast.LENGTH_SHORT).show();

                                            } else {
                                                mLeDeviceListAdapter.getDevice(i).setConnected(false);
                                                Toast.makeText(MainActivity.this, "断开连接", Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    }
                                    mLeDeviceListAdapter.notifyDataSetChanged();
                                }
                            });
                        }

                        @Override
                        public void onServicesDiscovered(BluetoothGatt gatt) {
                            super.onServicesDiscovered(gatt);
//                            if (QppApi.qppEnable(mBluetoothGatt, uuidQppService, uuidQppCharWrite)) {}
                            //设置notify
                            displayGattServices(gatt.getDevice().getAddress(), mBluetoothLeService.getSupportedGattServices(gatt.getDevice().getAddress()));
                        }

                        @Override
                        public void onChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
                            Log.e(TAG, "data===" + characteristic.getValue());
//                            QppApi.updateValueForNotification(gatt, characteristic);
                        }

                        @Override
                        public void onWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

                        }

                        @Override
                        public void onRead(BluetoothDevice device) {
                            super.onRead(device);
                            Log.e(TAG, "onRead");
                        }

                        @Override
                        public void onReady(BluetoothGatt gatt) {
                            super.onReady(gatt);
                            Log.e(TAG, "onReady");
//                            QppApi.setQppNextNotify(gatt, true);
                        }
                    });
                    //开始扫描
                    mBluetoothLeService.scanLeDevice(true);
                    break;
            }
        }
    };

    public boolean changeLevelInner(String address, int color) {
        byte[] data = new byte[Command.ComSyncColorLen];
        System.arraycopy(Command.ComSyncColor, 0, data, 0, Command.ComSyncColor.length);
        data[4] = (byte) (color & 0xff);
        data[5] = (byte) ((color >> 8) & 0xff);
        data[6] = (byte) ((color >> 16) & 0xff);
        data[7] = (byte) ((color >> 24) & 0xff);
        boolean result = mBluetoothLeService.wirteCharacteristic(address, mWriteCharacteristic, getWriteData(data));
        Log.e(TAG, "result==" + result);
        return result;
    }

    public byte[] getWriteData(byte[] data) {
//        byte[] bytes = new byte[data.length + 1];
        byte[] bytes = new byte[]{70, 127, -97, -80, -54, -117, 105, -76, -57};
        return bytes;
    }

    private BluetoothGattCharacteristic mWriteCharacteristic;//ble发送对象

    private void displayGattServices(String address, List<BluetoothGattService> gattServices) {
        if (gattServices == null)
            return;
        currentAddress = address;
        String uuid = null;
        // Loops through available GATT Services.
        for (BluetoothGattService gattService : gattServices) {
            uuid = gattService.getUuid().toString();
            Log.d(TAG, "displayGattServices: " + uuid);
            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();
            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) {
                uuid = gattCharacteristic.getUuid().toString();
                if (uuid.equals(BluetoothLeService.UUID_CHARACTERISTIC_TEXT)) {
                    Log.e("console", "2gatt Characteristic: " + uuid);
                    mWriteCharacteristic = gattCharacteristic;
                    mBluetoothLeService.setCharacteristicNotification(address,gattCharacteristic, true);//
//                    mBluetoothLeService.readCharacteristic(address,gattCharacteristic);//暂时注释
                }
            }
        }
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            mBluetoothLeService = ((BluetoothLeService.LocalBinder) service).getService();
            Log.e(TAG, "服务连接成功");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
            if (mBluetoothLeService != null) mHandler.sendEmptyMessage(1);
            // Automatically connects to the device upon successful start-up
            // initialization.
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unbindService(mServiceConnection);
        mBluetoothLeService = null;
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initBleDevice();//初始化蓝牙
        initView();
    }

    private void bindService() {
        Intent gattServiceIntent = new Intent(this, BluetoothLeService.class);
        boolean bll = bindService(gattServiceIntent, mServiceConnection,
                BIND_AUTO_CREATE);
        if (bll) {
            System.out.println("---------------");
        } else {
            System.out.println("===============");
        }
    }

    private void initView() {
        mListView = (ListView) findViewById(R.id.listView);
        mConnectedNum = (TextView) findViewById(R.id.connected_num);
        send = (Button) findViewById(R.id.sendData);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                changeLevelInner(currentAddress,-16717569);
            }
        });
        setConnectedNum();
        // Initializes list view adapter.
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                final BleDevice device = mLeDeviceListAdapter.getDevice(position);
                if (device == null) return;
//                final Intent intent = new Intent(MainActivity.this, DeviceControlActivity.class);
//                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_NAME, device.getName());
//                intent.putExtra(DeviceControlActivity.EXTRAS_DEVICE_ADDRESS, device.getAddress());
                if (mScanning) {
                    mBluetoothLeService.scanLeDevice(false);
                    mScanning = false;
                }
                if (device.isConnected()) {
                    mBluetoothLeService.disconnect(device.getBleAddress());
                } else {
                    mBluetoothLeService.connect(device.getBleAddress());
                }
            }
        });
    }

    public void initBleDevice() {
        // 检查设备是否支持BLE4.0
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        // 初始化 Bluetooth adapter, 通过蓝牙管理器得到一个参考蓝牙适配器(API必须在以上android4.3或以上和版本)
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();

        // 初始化蓝牙适配器. API版本必须是18以上, 通过 BluetoothManager 获取到BLE的适配器.

        // 检查当前的蓝牙设别是否支持.
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, R.string.ble_not_supported, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        } else {
            requestPermission(new String[]{Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION}, "请求设备权限", new GrantedResult() {
                @Override
                public void onResult(boolean granted) {
                    if (!granted) {
                        finish();
                    } else {
                        bindService();// 绑定服务
                    }
                }
            });
        }
    }

    private void setConnectedNum() {
        if (mBluetoothLeService != null) {
            mConnectedNum.setText("连接数量:" + mBluetoothLeService.getConnectedDevices().size());
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        if (!mScanning) {
            menu.findItem(R.id.menu_stop).setVisible(false);
            menu.findItem(R.id.menu_scan).setVisible(true);
            menu.findItem(R.id.menu_refresh).setActionView(null);
        } else {
            menu.findItem(R.id.menu_stop).setVisible(true);
            menu.findItem(R.id.menu_scan).setVisible(false);
            menu.findItem(R.id.menu_refresh).setActionView(
                    R.layout.actionbar_indeterminate_progress);
        }
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_scan:
                mLeDeviceListAdapter.clear();
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.scanLeDevice(true);
                }
                break;
            case R.id.menu_stop:
                if (mBluetoothLeService != null) {
                    mBluetoothLeService.scanLeDevice(false);
                }
                break;
        }
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        // User chose not to enable Bluetooth.
        if (requestCode == REQUEST_ENABLE_BT && resultCode == Activity.RESULT_CANCELED) {
            finish();
            return;
        } else {
            bindService();
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mBluetoothLeService != null) {
            mBluetoothLeService.scanLeDevice(false);
        }
        mLeDeviceListAdapter.clear();
    }

    private static byte[] reverseBytes(byte[] a) {
        int len = a.length;
        byte[] b = new byte[len];
        for (int k = 0; k < len; k++) {
            b[k] = a[a.length - 1 - k];
        }
        return b;
    }

    // byte转十六进制字符串
    public static String bytes2HexString(byte[] bytes) {
        String ret = "";
        for (int i = 0; i < bytes.length; i++) {
            String hex = Integer.toHexString(bytes[i] & 0xFF);
            if (hex.length() == 1) {
                hex = '0' + hex;
            }
            ret += hex.toUpperCase();
        }
        return ret;
    }

}
