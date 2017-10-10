package cn.com.heaton.blelibrary.spp;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.BuildConfig;

/**
 * 蓝牙管理器
 * 针对蓝牙SPP 3.0
 * Created by LiuLei on 2017/9/14.
 */
public class BtManager implements Handler.Callback {
	public final static  String TAG           = BtManager.class.getSimpleName();
	private static final String NAME_SECURE   = "BluetoothChatSecure";
	private static final String NAME_INSECURE = "BluetoothChatInsecure";
	public final static  int    STATE_IDLE    = 0xb01;//服务状态空闲
	public final static  int    STATE_SERVING = 0xb02;//服务状态运行中
	public final static  int    STATE_FINISH  = 0xb03;//服务状态结束

	public final static int MSG_STATE_CHANGED  = 0xc01;//设备状态变更
	public final static int MSG_DEVICE_CHANGED = 0xc02;//设备数量变更
	public final static int MSG_READ_DATA      = 0xc03;//读取数据
	public final static int MSG_WRITE_DATA     = 0xc04;//写入数据
	public final static int MSG_CLOSE          = 0xc05;//关闭设备
	public final static int MSG_STOP_SCAN      = 0xc06;//停止扫描


	private final List<BtDevice> mDevices           = new ArrayList<>();//所有设备列表
	private final List<BtDevice> mWaitingDevices    = new ArrayList<>();//等待连接设备
	private final List<BtDevice> mConnectingDevices = new ArrayList<>();//连接中的设备
	private final List<BtDevice> mConnectedDevices  = new ArrayList<>();//连接成功的设备
	private BtActionTask     mBtActionTask;//设备连接任务
	private BtDeviceListener mBtDeviceListener;//设备监听器
	private Context          mContext;//程序对象
	private BtServerThread   mBtServerThread;//蓝牙服务线程
	private Handler          mHandler;//主线程对象
	private boolean          mConnecting;//是否连接中
	private BluetoothAdapter mBluetoothAdapter;//蓝牙适配器

	private final Object  mLocker     = new Object();//线程锁
	private       int     mState      = STATE_IDLE;//默认状态空闲
	private       boolean mRegistered = false;//是否已注册服务
	private       boolean mSecure     = true;//是否加密

	public BtManager(Context context, BtDeviceListener btDeviceListener) {
		mContext = context;
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		mBtDeviceListener = btDeviceListener;

		if (mBluetoothAdapter != null) {
			mState = STATE_SERVING;
			mBtServerThread = new BtServerThread();
			mBtServerThread.start();
		}
		mHandler = new Handler(mContext.getMainLooper(), this);
	}

	/**
	 * 设置加密
	 *
	 * @param secure 是否加密
	 */
	public void setSecure(boolean secure) {
		if (mSecure != secure) {
			mSecure = secure;
		}
	}

	/**
	 * 连接设备
	 *
	 * @param device 设备对象
	 */
	public void connect(BtDevice device) {
		if (mBluetoothAdapter == null) {
			return;
		}
		synchronized (mLocker) {
			if (device == null || device.isConnected() || device.isConnecting() || !mDevices.contains(device) || mWaitingDevices.contains(device) || mConnectingDevices.contains(device) || mConnectedDevices.contains(device)) {
				return;
			}
			mWaitingDevices.add(device);
		}
		if (mBtActionTask == null || mBtActionTask.isCancelled()) {
			mBtActionTask = new BtActionTask();
		}
		if (!mConnecting) {
			mConnecting = true;
			mBtActionTask.execute();
		}
	}

	public void disconnect(BtDevice device){

	}

	/**
	 * 删除设备
	 *
	 * @param device 设备对象
	 */
	public void delete(BtDevice device) {
		if (device == null) {
			return;
		}
		device.setDelete(true);
		if (!device.isConnected() && !device.isConnecting()) {
			synchronized (mLocker) {
				if (mWaitingDevices.contains(device)) {
					mWaitingDevices.remove(device);
				}
				if (mConnectingDevices.contains(device)) {
					mConnectingDevices.remove(device);
				}
				if (mConnectedDevices.contains(device)) {
					mConnectedDevices.remove(device);
				}
				if (mDevices.contains(device)) {
					mDevices.remove(device);
				}
			}
		} else {
			device.close();
		}
	}

	/**
	 * 获取设备对象列表
	 *
	 * @return 设备数组
	 */
	public List<BtDevice> getDevices() {
		return mDevices;
	}

	/**
	 * 获取已连接设备列表
	 *
	 * @return 设备数组
	 */
	public List<BtDevice> getConnectedDevices() {
		return mConnectedDevices;
	}

	/**
	 * 获取线程锁
	 *
	 * @return 线程锁
	 */
	public Object getLocker() {
		return mLocker;
	}

	public Handler getHandler() {
		return mHandler;
	}

	@Override
	public boolean handleMessage(Message msg) {
		if (msg != null) {
			switch (msg.what) {
				case MSG_STATE_CHANGED://设备状态变更
					if (mBtDeviceListener != null) {
						mBtDeviceListener.onStateChanged(msg.arg1, getDevice(msg.arg2));
					}
					break;
				case MSG_DEVICE_CHANGED://设备数量变更
					if (mBtDeviceListener != null) {
						mBtDeviceListener.onDevicesChanged();
					}
					break;
				case MSG_READ_DATA://设备数据读取
					if (mBtDeviceListener != null) {
						mBtDeviceListener.onRead((byte[]) msg.obj, getDevice(msg.arg1));
					}
					break;
				case MSG_WRITE_DATA://设备数据写入
					if (mBtDeviceListener != null) {
						mBtDeviceListener.onWrite((byte[]) msg.obj, getDevice(msg.arg1));
					}
					break;
				case MSG_CLOSE://关闭设备
					BtDevice btDevice = getDevice(msg.arg1);
					if (btDevice != null) {
						btDevice.closeInner();
					}
					break;
				case MSG_STOP_SCAN://停止扫描
					cancelDiscovery();
					break;
			}
		}
		return false;
	}

	/**
	 * 设备数量变更
	 */
	void onDevicesChanged() {
		if (mHandler != null) {
			mHandler.obtainMessage(MSG_DEVICE_CHANGED).sendToTarget();
		}
	}

	/**
	 * 获取设备对象
	 *
	 * @param index 自增ID
	 * @return 设备对象
	 */
	private BtDevice getDevice(int index) {
		synchronized (mLocker) {
			for (BtDevice btDevice : mDevices) {
				if (btDevice.getIndex() == index) {
					return btDevice;
				}
			}
		}
		return null;
	}

	/**
	 * 设备对象断开
	 *
	 * @param device 设备对象
	 */
	void onDisconnected(BtDevice device) {
		if (device == null) {
			return;
		}
		synchronized (mLocker) {
			if (mWaitingDevices.contains(device)) {
				mWaitingDevices.remove(device);
			}
			if (mConnectingDevices.contains(device)) {
				mConnectingDevices.remove(device);
			}
			if (mConnectedDevices.contains(device)) {
				mConnectedDevices.remove(device);
			}
			onStateChanged(BtDevice.STATE_IDLE, device);
			if (device.getDelete()) {
				mDevices.remove(device);
			}
		}
	}

	/**
	 * 设备对象状态变更
	 *
	 * @param state  新状态
	 * @param device 设备对象
	 */
	void onStateChanged(int state, BtDevice device) {
		if (mHandler != null) {
			mHandler.obtainMessage(MSG_STATE_CHANGED, state, device.getIndex()).sendToTarget();
		}
		if (state == BtDevice.STATE_CONNECTED) {
			synchronized (mLocker) {
				mConnectingDevices.remove(device);
				mConnectedDevices.add(device);
			}
		} else if (state == BtDevice.STATE_IDLE) {
			synchronized (mLocker) {
				mConnectedDevices.remove(device);
				mConnectingDevices.remove(device);
			}
		} else if (state == BtDevice.STATE_CONNECTING) {
			synchronized (mLocker) {
				mConnectedDevices.remove(device);
			}
		}
	}

	/**
	 * 关闭设备
	 *
	 * @param btDevice 设备对象
	 */
	void close(BtDevice btDevice) {
		if (mHandler != null && btDevice != null) {
			mHandler.obtainMessage(MSG_CLOSE, btDevice.getIndex(), 0).sendToTarget();
		}
	}

	/**
	 * 添加设备
	 *
	 * @param device 设备对象
	 */
	public void addDevice(BtDevice device) {
		if (mBluetoothAdapter == null) {
			return;
		}
		if (device == null) {
			return;
		}
		synchronized (mLocker) {
			if (!contains(device)) {
				mDevices.add(device);
				onDevicesChanged();
			}
		}
	}

	/**
	 * 开始扫描
	 */
	public void startDiscovery() {
		if (mHandler != null) {
			mHandler.removeMessages(MSG_STOP_SCAN);
		}
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
			if (!mRegistered) {
				mRegistered = true;
				IntentFilter filter = new IntentFilter();
				filter.addAction(BluetoothDevice.ACTION_FOUND);
				filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
				mContext.registerReceiver(mReceiver, filter);
			}
			mBluetoothAdapter.startDiscovery();
			if (mBtDeviceListener != null) {
				mBtDeviceListener.onStartScan();
			}
			if (mHandler != null) {
				mHandler.sendEmptyMessageDelayed(MSG_STOP_SCAN, 12000);
			}
		}
	}

	/**
	 * 取消扫描
	 */
	public void cancelDiscovery() {
		if (mBluetoothAdapter != null) {
			if (mBluetoothAdapter.isDiscovering()) {
				mBluetoothAdapter.cancelDiscovery();
			}
		}
	}

	/**
	 * 释放管理器
	 */
	public void release() {
		synchronized (mLocker) {
			mState = STATE_FINISH;
			for (BtDevice btDevice : mDevices) {
				btDevice.close();
			}
			mDevices.clear();
			mWaitingDevices.clear();
			mConnectingDevices.clear();
			if (mBtServerThread != null) {
				mBtServerThread.stopServing();
				mBtServerThread = null;
			}
			mHandler = null;
		}
	}

	/**
	 * 添加设备
	 *
	 * @param device 系统蓝牙设备对象
	 * @return 初始化的设备对象
	 */
	public BtDevice addDevice(BluetoothDevice device) {
		if (mBluetoothAdapter == null) {
			return null;
		}
		if (device == null) {
			return null;
		}
		synchronized (mLocker) {
			if (!contains(device)) {
				BtDevice btDevice = new BtDevice(this, device);
				mDevices.add(btDevice);
				onDevicesChanged();
				return btDevice;
			} else {
				return getDevice(device);
			}
		}
	}

	/**
	 * 获取设备对象
	 *
	 * @param device 系统蓝牙设备对象
	 * @return 初始化的蓝牙设备
	 */
	public BtDevice getDevice(BluetoothDevice device) {
		if (device == null) {
			return null;
		}
		synchronized (mLocker) {
			for (BtDevice btDevice : mDevices) {
				if (device.equals(btDevice.getBluetoothDevice())) {
					return btDevice;
				}
			}
		}
		return null;
	}

	/**
	 * 是否包含设备
	 *
	 * @param device 蓝牙设备
	 * @return 是否包含
	 */
	public boolean contains(BtDevice device) {
		synchronized (mLocker) {
			return mDevices.contains(device);
		}
	}

	/**
	 * 是否包含设备
	 *
	 * @param device 系统蓝牙设备对象
	 * @return 是否包含
	 */
	public boolean contains(BluetoothDevice device) {
		synchronized (mLocker) {
			for (BtDevice btDevice : mDevices) {
				if (btDevice.getAddress().equalsIgnoreCase(device.getAddress())) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * 读取设备数据
	 *
	 * @param btDevice 设备对象
	 * @param data     读取的数据
	 */
	void onReadData(BtDevice btDevice, byte[] data) {
		if (mHandler != null) {
			mHandler.obtainMessage(MSG_READ_DATA, btDevice.getIndex(), 0, data).sendToTarget();
		}
	}

	/**
	 * 写入设备数据
	 *
	 * @param btDevice 设备对象
	 * @param data     写入的数据
	 */
	void onWriteData(BtDevice btDevice, byte[] data) {
		if (mHandler != null) {
			mHandler.obtainMessage(MSG_WRITE_DATA, btDevice.getIndex(), 0, data).sendToTarget();
		}
	}

	public BluetoothAdapter getAdapter() {
		return mBluetoothAdapter;
	}

	/**
	 * 连接蓝牙设备的任务管理器
	 */
	private class BtActionTask extends AsyncTask<Void, Void, Void> {

		@Override
		protected Void doInBackground(Void... params) {
			if (mWaitingDevices.size() == 0) {
				return null;
			}
			mConnecting = true;

			// 取消扫描.因为会影响设备的链接速度
			cancelDiscovery();

			BtDevice device;
			synchronized (mLocker) {
				device = mWaitingDevices.remove(0);
				mConnectingDevices.add(device);
			}

			while (mConnecting && device != null) {
				device.connect(mSecure);
//				if (success) {
//					synchronized (mLocker) {
//						mConnectingDevices.remove(device);
//						mConnectedDevices.add(device);
//						onStateChanged(BtDevice.STATE_CONNECTED, device);
//					}
//				}

				synchronized (mLocker) {
					if (mWaitingDevices.size() > 0) {
						device = mWaitingDevices.remove(0);
						mConnectingDevices.add(device);
					} else {
						device = null;
						mConnecting = false;
					}
				}
			}

			return null;
		}

		@Override
		protected void onCancelled() {
			super.onCancelled();
			mConnecting = false;
			mBtActionTask = null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			super.onPostExecute(aVoid);
			mConnecting = false;
			mBtActionTask = null;
		}
	}

	/**
	 * 蓝牙服务端运行的线程
	 */
	class BtServerThread extends Thread {

		BluetoothServerSocket mServerSocket = null;

		@Override
		public void run() {
			super.run();
			BluetoothServerSocket bluetoothServerSocket;
			// 创建一个服务的Listener
			try {
				if (mSecure) {
					bluetoothServerSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(NAME_SECURE, BtConfig.UUID_SECURE);
				} else {
					bluetoothServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(NAME_INSECURE, BtConfig.UUID_INSECURE);
				}
			} catch (IOException e) {
				Log.e(TAG, "Socket Type: mSecure=" + mSecure + ",listen() failed", e);
				return;
			}

			BluetoothSocket socket;
			// 如果没有进入已链接状态的话.会持续的进入此操作
			while (mState == STATE_SERVING) {
				try {
					// 这是一个阻塞的链接.除非链接完成.或者是发生异常
					socket = bluetoothServerSocket.accept();
				} catch (IOException e) {
					Log.e(TAG, "Socket Type: mSecure=" + mSecure + ",accept() failed", e);
					break;
				}
				// 如果一个连接被接受
				if (socket != null) {
					BluetoothDevice device = socket.getRemoteDevice();
					addDevice(device);
					BtDevice btDevice = getDevice(device);
					if (btDevice != null) {
						btDevice.setMaster(true);
						btDevice.setBluetoothSocket(socket);
						onStateChanged(BtDevice.STATE_IDLE, btDevice);
					}
					break;
				}
			}
		}

		public void stopServing() {
			if (mServerSocket != null) {
				try {
					mServerSocket.close();
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
				}
			}
			interrupt();
		}
	}

	/**
	 * 蓝牙广播接受者
	 */
	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "action:" + action);
			}
			// 发现一个设备
			if (BluetoothDevice.ACTION_FOUND.equals(action)) {
				// 获取设备对象
				BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
				if (!TextUtils.isEmpty(device.getName()) && !device.getName().equalsIgnoreCase("null")) {
					BtDevice btDevice = addDevice(device);
					if (btDevice != null) {
						if (mBtDeviceListener != null) {
							mBtDeviceListener.onFound(btDevice);
						}
					}
				}
			} else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
				if (mRegistered) {
					mRegistered = false;
					mContext.unregisterReceiver(mReceiver);
				}
				if (mBtDeviceListener != null) {
					mBtDeviceListener.onStopScan();
				}
			}
		}
	};

	/**
	 * 蓝牙监听器
	 */
	public interface BtDeviceListener {
		public void onStateChanged(int state, BtDevice btDevice);

		public void onDevicesChanged();

		public void onError(int errorCode, BtDevice btDevice);

		public void onRead(byte[] buffer, BtDevice btDevice);

		public void onWrite(byte[] buffer, BtDevice btDevice);

		public void onFound(BtDevice btDevice);

		public void onStartScan();

		public void onStopScan();
	}
}
