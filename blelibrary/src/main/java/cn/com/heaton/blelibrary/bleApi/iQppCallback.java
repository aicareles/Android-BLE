package cn.com.heaton.blelibrary.bleApi;

import android.bluetooth.BluetoothGatt;

/**
 * @Description QPP API / CallBack functions
 * @author liulei
 * @version 1.0
 * @date 2016-11-27
 * @Copyright (c) 2016 Quintic Co., Ltd. Inc. All rights reserved.
 *
 */

public interface iQppCallback {
	void onQppReceiveData(BluetoothGatt mBluetoothGatt, String qppUUIDForNotifyChar, byte[] qppData);
}
