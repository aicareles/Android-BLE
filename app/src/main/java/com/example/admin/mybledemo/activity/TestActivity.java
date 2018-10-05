package com.example.admin.mybledemo.activity;

import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.admin.mybledemo.adapter.LeDeviceListAdapter;
import com.example.admin.mybledemo.R;
import com.example.admin.mybledemo.annotation.ContentView;
import com.example.admin.mybledemo.annotation.OnItemClick;
import com.example.admin.mybledemo.annotation.ViewInit;
import com.example.admin.mybledemo.command.AppProtocol;
import com.example.admin.mybledemo.command.CommandBean;

import java.util.Arrays;
import java.util.UUID;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.ble.callback.BleConnCallback;
import cn.com.heaton.blelibrary.ble.callback.BleNotiftCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadRssiCallback;
import cn.com.heaton.blelibrary.ble.callback.BleScanCallback;

@ContentView(R.layout.activity_test)
public class TestActivity extends BaseActivity {

    private static final String TAG = "TestActivity";
    @ViewInit(R.id.lv_scan)
    private ListView mListView;
    @ViewInit(R.id.notify_statue)
    private TextView mNotifyStatus;
    @ViewInit(R.id.notify_value)
    private TextView mNotifyValue;

    private LeDeviceListAdapter mLeDeviceListAdapter;
    private Ble<BleDevice> mBle;
    private BleDevice mDevice;

    @Override
    protected void onInitView() {
        //根据自身需求传入需要在其他界面操作的蓝牙对象  这里测试取第一个设备对象
        mBle = Ble.getInstance();
        mDevice = mBle.getConnetedDevices().get(0);
        mLeDeviceListAdapter = new LeDeviceListAdapter(this);
        mListView.setAdapter(mLeDeviceListAdapter);
    }

    @Override
    protected void initLinsenter() {}

    //测试通知
    public void testNotify(View view) {
        if(mDevice != null){
            mNotifyStatus.setText("设置通知监听成功！！！");
            mBle.startNotify(mDevice, mBleNotifyCallback);
        }
    }

    //测试扫描
    public void testScan(View view){
        if (mBle != null && !mBle.isScanning()) {
            mLeDeviceListAdapter.clear();
            mLeDeviceListAdapter.addDevices(mBle.getConnetedDevices());
            mBle.startScan(new BleScanCallback<BleDevice>() {
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
            });
        }
    }

    //测试发送
    public void testSend(View view){
        if(mDevice != null){
            //发送数据
            CommandBean commandBean = new CommandBean();
            AppProtocol.sendCarCmdCommand(mDevice, commandBean.setCarCommand(80, 1));
        }
    }

    //测试读取rssi值
    public void testRssi(View view){
        if(mDevice != null) {
            mBle.readRssi(mDevice, new BleReadRssiCallback<BleDevice>() {
                @Override
                public void onReadRssiSuccess(int rssi) {
                    super.onReadRssiSuccess(rssi);
                    Log.e(TAG, "onReadRssiSuccess: " + rssi);
                    Toast.makeText(TestActivity.this, "onReadRssiSuccess:" + rssi, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @OnItemClick(R.id.lv_scan)
    public void itemOnClick(AdapterView<?> parent, View view, int position, long id){
        //测试连接或断开
        final BleDevice device = mLeDeviceListAdapter.getDevice(position);
        if (device == null) return;
        if (mBle.isScanning()) {
            mBle.stopScan();
        }
        if (device.isConnected()) {
//          mBle.disconnect(device, connectCallback);
            mBle.disconnect(device);
        } else if (!device.isConnectting()) {
            mBle.connect(device, connectCallback);
        }
    }

    /*连接的回调*/
    private BleConnCallback<BleDevice> connectCallback = new BleConnCallback<BleDevice>() {
        @Override
        public void onConnectionChanged(BleDevice device) {
            if (device.isConnected()) {
                setNotify(device);
            }
            Log.e(TAG, "onConnectionChanged: " + device.isConnected());
            mLeDeviceListAdapter.notifyDataSetChanged();
        }

        @Override
        public void onConnectException(BleDevice device, int errorCode) {
            super.onConnectException(device, errorCode);
            Toast.makeText(TestActivity.this, "连接异常，异常状态码:" + errorCode, Toast.LENGTH_SHORT).show();
        }
    };

    /*设置通知的回调*/
    private void setNotify(BleDevice device) {
         /*连接成功后，设置通知*/
        mBle.startNotify(device, mBleNotifyCallback);
    }

    private BleNotiftCallback<BleDevice> mBleNotifyCallback = new BleNotiftCallback<BleDevice>() {
        @Override
        public void onChanged(BleDevice device, BluetoothGattCharacteristic characteristic) {
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
    };
}
