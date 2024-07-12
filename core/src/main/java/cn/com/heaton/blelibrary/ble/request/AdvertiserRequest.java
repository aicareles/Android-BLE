package cn.com.heaton.blelibrary.ble.request;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.Handler;
import androidx.annotation.RequiresApi;

import cn.com.heaton.blelibrary.ble.BleLog;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.utils.ThreadUtils;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.exception.AdvertiserUnsupportException;

/**
 * TODO 2020/02/01
 * description $desc$
 * created by jerry on 2019/02/21.
 */

@Implement(AdvertiserRequest.class)
@TargetApi(Build.VERSION_CODES.LOLLIPOP)
public class AdvertiserRequest<T extends BleDevice> {
    private static final String TAG = "AdvertiserRequest";

    private Handler mHandler = BleHandler.of();
    private BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseSettings myAdvertiseSettings;
    private AdvertiseData myAdvertiseData;

    private void setAdvertiserSettings(){
        mAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        if (mAdvertiser == null) {
            try {
                throw new AdvertiserUnsupportException("Device does not support Avertise!");
            } catch (AdvertiserUnsupportException e) {
                e.printStackTrace();
            }
        }
        //设置频率:  ADVERTISE_MODE_LOW_LATENCY 100ms     ADVERTISE_MODE_LOW_POWER 1s     ADVERTISE_MODE_BALANCED  250ms
        myAdvertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)//设置广播间隔100ms
                .setConnectable(true)
                .setTimeout(0)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .build();
    }

    public void startAdvertising(final byte[] payload, final AdvertiseSettings advertiseSettings){
        if (bluetoothAdapter.isEnabled()){
            mHandler.removeCallbacks(stopAvertiseRunnable);
            if(mAdvertiser != null){
                ThreadUtils.asyn(new Runnable() {
                    @Override
                    public void run() {
                        mAdvertiser.stopAdvertising(mAdvertiseCallback);
                        myAdvertiseData = new AdvertiseData.Builder()
                                .addManufacturerData(65520, payload)
                                .setIncludeDeviceName(true)
                                .build();
                        mAdvertiser.startAdvertising(advertiseSettings, myAdvertiseData, mAdvertiseCallback);
                    }
                });
            }
        }
    }

    public void startAdvertising(byte[] payload) {
        setAdvertiserSettings();
        startAdvertising(payload, myAdvertiseSettings);
    }

    public void stopAdvertising() {
        if (bluetoothAdapter.isEnabled()){
            if(mAdvertiser != null){
                ThreadUtils.asyn(new Runnable() {
                    @Override
                    public void run() {
                        BleLog.d(TAG, "stopAdvertising: 停止广播");
                        mAdvertiser.stopAdvertising(mAdvertiseCallback);
                    }
                });
            }
        }
    }

    public void stopAdvertising(Long delay){
        mHandler.postDelayed(stopAvertiseRunnable, delay);
    }

    private Runnable stopAvertiseRunnable = new Runnable() {
        @Override
        public void run() {
            stopAdvertising();
        }
    };

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            BleLog.d(TAG, "onStartSuccess: 开启广播成功");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                BleLog.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                BleLog.e(TAG, "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                BleLog.e(TAG, "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                BleLog.e(TAG, "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                BleLog.e(TAG, "This feature is not supported on this platform");
            }
        }
    };
}
