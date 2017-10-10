package com.example.admin.mybledemo;

import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

/**
 * 蓝牙配对接收器
 */

public class BluetoothConnectReceiver extends BroadcastReceiver {
	public static final String TAG = "BluetoothReceiver";

	String strPsw = "0000";

	@Override
	public void onReceive(Context context, Intent intent) {
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "action=" + intent.getAction());
		}
		if (intent.getAction().equals("android.bluetooth.device.action.PAIRING_REQUEST")) {
			BluetoothDevice btDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
			try {
//				BtUtils.setPin(btDevice.getClass(), btDevice, strPsw); // 手机和蓝牙采集器配对
//				BtUtils.createBond(btDevice.getClass(), btDevice);
//				BtUtils.cancelPairingUserInput(btDevice.getClass(), btDevice);
//				byte[] pinBytes = strPsw.getBytes("UTF-8");
//
//				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//					btDevice.setPin(pinBytes);
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
}

