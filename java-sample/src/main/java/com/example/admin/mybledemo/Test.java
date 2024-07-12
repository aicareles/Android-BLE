package com.example.admin.mybledemo;

import android.Manifest;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothGattCharacteristic;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.callback.BleMtuCallback;
import cn.com.heaton.blelibrary.ble.callback.BleReadCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteCallback;
import cn.com.heaton.blelibrary.ble.callback.BleWriteEntityCallback;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.EntityData;
import cn.com.heaton.blelibrary.ble.queue.RequestTask;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;
import cn.com.heaton.blelibrary.ble.utils.CrcUtils;
import cn.com.superLei.aoparms.annotation.Permission;
import cn.com.superLei.aoparms.annotation.Retry;

public class Test extends AppCompatActivity {
    private static final String TAG = "Test";
    private Ble<BleDevice> ble = Ble.getInstance();

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
        Log.e(TAG, "sendData: " + ByteUtils.toHexString(data));
//        return ble.write(list.get(0), "Hello Android!".getBytes(), new BleWriteCallback<BleDevice>() {
        ble.write(ble.getConnectedDevices().get(0), data, new BleWriteCallback<BleDevice>() {
            @Override
            public void onWriteSuccess(BleDevice device, BluetoothGattCharacteristic characteristic) {
                Log.e(TAG, "onWriteSuccess: ");
            }

            @Override
            public void onWriteFailed(BleDevice device, int failedCode) {
                Log.e(TAG, "onWiteFailed: " + failedCode);
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
     * 设置请求MTU
     */
    private void requestMtu() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
            ble.setMTU(ble.getConnectedDevices().get(0).getBleAddress(), 96, new BleMtuCallback<BleDevice>() {
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

    private void writeQueue() {
        List<BleDevice> connectedDevices = ble.getConnectedDevices();
        for (int i = 0; i < 10; i++) {
            RequestTask requestTask = new RequestTask.Builder()
                    .devices(connectedDevices.toArray(new BleDevice[]{}))
                    .data("hello android".getBytes())
                    .delay(500)
                    .build();
            ble.writeQueue(requestTask);
        }
    }


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
        BleDevice device = ble.getConnectedDevices().get(0);
        return new EntityData.Builder()
                .setAutoWriteMode(autoWriteMode)
                .setAddress(device.getBleAddress())
                .setData(data)
                .setPackLength(20)
                .setDelay(50L)
                .build();
    }


}
