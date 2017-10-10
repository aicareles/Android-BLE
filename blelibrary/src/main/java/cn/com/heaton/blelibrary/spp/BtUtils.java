package cn.com.heaton.blelibrary.spp;

import android.bluetooth.BluetoothDevice;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * 蓝牙功能
 * Created by LiuLei on 2017/9/14.
 */

public class BtUtils {

	public static boolean createBond(Class<?> btClass, BluetoothDevice btDevice) throws Exception {
		Method createBondMethod = btClass.getMethod("createBond");
		return (Boolean) createBondMethod.invoke(btDevice);
	}

	public static boolean removeBond(Class<?> btClass, BluetoothDevice btDevice)
			throws Exception {
		Method removeBondMethod = btClass.getMethod("removeBond");
		return (Boolean) removeBondMethod.invoke(btDevice);
	}

	static public boolean setPin(Class<?> btClass, BluetoothDevice btDevice,
	                             String str) throws Exception {
		try {
			Method removeBondMethod = btClass.getDeclaredMethod("setPin", byte[].class);
			return (Boolean) removeBondMethod.invoke(btDevice, new Object[]{str.getBytes()});
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;

	}

	// 取消用户输入
	public static boolean cancelPairingUserInput(Class<?> btClass, BluetoothDevice device) throws Exception {
		Method createBondMethod = btClass.getMethod("cancelPairingUserInput");
		// cancelBondProcess()
		return (Boolean) createBondMethod.invoke(device);
	}

	// 取消配对
	public static boolean cancelBondProcess(Class<?> btClass, BluetoothDevice device) throws Exception {
		Method createBondMethod = btClass.getMethod("cancelBondProcess");
		return (Boolean) createBondMethod.invoke(device);
	}

	static public void printAllInform(Class clsShow) {
		try {
			// 取得所有方法
			Method[] hideMethod = clsShow.getMethods();
			int i = 0;
			for (; i < hideMethod.length; i++) {
				Log.e("method name", hideMethod[i].getName() + ";and the i is:"
				                     + i);
			}
			// 取得所有常量
			Field[] allFields = clsShow.getFields();
			for (i = 0; i < allFields.length; i++) {
				Log.e("Field name", allFields[i].getName());
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static boolean pair(BtDevice btDevice) {
		boolean result = false;

		BluetoothDevice device = btDevice.getBluetoothDevice();

		if (device.getBondState() != BluetoothDevice.BOND_BONDED) {
			try {
				setPin(device.getClass(), device, "0000"); // 手机和蓝牙采集器配对
				createBond(device.getClass(), device);
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
			}

		} else {
			try {
				createBond(device.getClass(), device);
				setPin(device.getClass(), device, "0000"); // 手机和蓝牙采集器配对
				createBond(device.getClass(), device);
				result = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return result;
	}

}
