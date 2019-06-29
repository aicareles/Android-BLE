package cn.com.heaton.blelibrary.ble.request;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.os.Build;
import android.os.Handler;
import android.support.annotation.RequiresApi;

import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.BleHandler;
import cn.com.heaton.blelibrary.ble.L;
import cn.com.heaton.blelibrary.ble.utils.TaskExecutor;
import cn.com.heaton.blelibrary.ble.annotation.Implement;
import cn.com.heaton.blelibrary.ble.exception.AdvertiserUnsupportException;

/**
 * description $desc$
 * created by jerry on 2019/02/21.
 */

@Implement(AdvertiserRequest.class)
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class AdvertiserRequest<T extends BleDevice> {
    private static final String TAG = "AdvertiserRequest";

    private Handler mHandler;
    private BluetoothAdapter bluetoothAdapter;
    private BluetoothLeAdvertiser mAdvertiser;
    private AdvertiseSettings myAdvertiseSettings;
    private AdvertiseData myAdvertiseData;

    protected AdvertiserRequest() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        mAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
        mHandler = BleHandler.of();
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

    public void startAdvertising(final byte[] payload) {
        mHandler.removeCallbacks(stopAvertiseRunnable);
        if(mAdvertiser != null){
            TaskExecutor.executeTask(new Runnable() {
                @Override
                public void run() {
                    mAdvertiser.stopAdvertising(mAdvertiseCallback);
                    myAdvertiseData = new AdvertiseData.Builder()
                            .addManufacturerData(65520, payload)
                            .setIncludeDeviceName(true)
                            .build();
                    mAdvertiser.startAdvertising(myAdvertiseSettings, myAdvertiseData, mAdvertiseCallback);
                }
            });
        }
    }

    public void stopAdvertising() {
        if(mAdvertiser != null){
            TaskExecutor.executeTask(new Runnable() {
                @Override
                public void run() {
                    L.e(TAG, "stopAdvertising: 停止广播");
                    mAdvertiser.stopAdvertising(mAdvertiseCallback);
                }
            });
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

    private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect) {
            super.onStartSuccess(settingsInEffect);
            L.e(TAG, "onStartSuccess: 开启广播成功");
        }

        @Override
        public void onStartFailure(int errorCode) {
            super.onStartFailure(errorCode);
            if (errorCode == ADVERTISE_FAILED_DATA_TOO_LARGE) {
                L.e(TAG, "Failed to start advertising as the advertise data to be broadcasted is larger than 31 bytes.");
            } else if (errorCode == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                L.e(TAG, "Failed to start advertising because no advertising instance is available.");
            } else if (errorCode == ADVERTISE_FAILED_ALREADY_STARTED) {
                L.e(TAG, "Failed to start advertising as the advertising is already started");
            } else if (errorCode == ADVERTISE_FAILED_INTERNAL_ERROR) {
                L.e(TAG, "Operation failed due to an internal error");
            } else if (errorCode == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                L.e(TAG, "This feature is not supported on this platform");
            }
        }
    };
}
