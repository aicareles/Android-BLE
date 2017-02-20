package cn.com.heaton.blelibrary.bleApi;

import android.bluetooth.BluetoothGatt;

/**This class is un_used current
 * Created by liulei on 2016/11/29.
 */

public interface iQppCallback {
	void onQppReceiveData(BluetoothGatt mBluetoothGatt, String qppUUIDForNotifyChar, byte[] qppData);
}
