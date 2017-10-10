package cn.com.heaton.blelibrary.spp;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.text.TextUtils;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import cn.com.heaton.blelibrary.BuildConfig;

/**
 * 蓝牙设备对象
 * Created by LiuLei on 2017/9/14.
 */

public class BtDevice {
	public final static String TAG = BtDevice.class.getSimpleName();

	private final static int WRITE_TIMEOUT = 3000;// 写入超时时间

	public final static int STATE_IDLE       = 0xa01;
	public final static int STATE_CONNECTING = 0xa02;
	public final static int STATE_CONNECTED  = 0xa03;

	private static int mDeviceIndex = 1;

	private int mState = STATE_IDLE;
	private boolean         mSecure;//是否加密
	private boolean         mReading;//是否正在读取数据
	private BluetoothDevice mDevice;//绑定的系统蓝牙设备对象
	private BluetoothSocket mBluetoothSocket;//连接的蓝牙套字节
	private BtManager       mBtManager;//蓝牙设备管理器
	private ReadThread      mReadThread;//读取数据线程
	private ConnectThread   mConnectThread;//连接线程
	private int             mIndex;//设备ID
	private String          mName;//设备名称
	private String          mAlias;//设备别名
	private boolean         mDelete;//是否删除
	private boolean         mMaster;//是否是服务端设备
	private final Object  mLocker  = new Object();//写入锁
	private       boolean mRewrite = false;//重新写入


	public BtDevice(BtManager manager, BluetoothDevice device) {
		mBtManager = manager;
		mDevice = device;
		mIndex = mDeviceIndex++;
	}

	/**
	 * 获取蓝牙设备地址
	 *
	 * @return 蓝牙地址
	 */
	public String getAddress() {
		return mDevice != null ? mDevice.getAddress() : "";
	}

	/**
	 * 设置蓝牙设备名称
	 *
	 * @param name 设备名称
	 */
	public void setName(String name) {
		mName = name;
	}

	/**
	 * 设置蓝牙设备别名
	 *
	 * @param alias 设备别名
	 */
	public void setAlias(String alias) {
		mAlias = alias;
	}

	/**
	 * 获取蓝牙设备别名
	 *
	 * @return 设备别名
	 */
	public String getAlias() {
		return mAlias;
	}

	/**
	 * 设置删除设备
	 *
	 * @param delete 是否删除
	 */
	void setDelete(boolean delete) {
		mDelete = delete;
	}

	/**
	 * 获取删除状态
	 *
	 * @return 是否删除
	 */
	boolean getDelete() {
		return mDelete;
	}

	/**
	 * 设置是否是主设备
	 *
	 * @param master 主设备
	 */
	void setMaster(boolean master) {
		mMaster = master;
	}

	/**
	 * 获取是否是主设备
	 *
	 * @return 主设备
	 */
	public boolean isMaster() {
		return mMaster;
	}

	/**
	 * 获取设备名称
	 *
	 * @return 设备名称
	 */
	public String getName() {
		String name = mDevice != null ? mDevice.getName() : "";
		if (!TextUtils.isEmpty(name)) {
			return name;
		}
		return mName == null ? "" : mName;
	}

	/**
	 * 是否正在连接
	 *
	 * @return 正在连接
	 */
	public boolean isConnecting() {
		return mState == STATE_CONNECTING;
	}

	/**
	 * 是否已连接
	 *
	 * @return 已连接
	 */
	public boolean isConnected() {
		return mState == STATE_CONNECTED;
	}


	/**
	 * 获取设备ID
	 *
	 * @return 设备ID
	 */
	public int getIndex() {
		return mIndex;
	}

	/**
	 * 写入设备数据
	 *
	 * @param data 写入的数据
	 * @return 是否写入成功
	 */
	public boolean writeData(byte[] data) {
		return writeData(data, false);
	}

	/**
	 * 释放写入锁
	 */
	public void notifyWrite() {
		notifyWrite(false);
	}

	/**
	 * 释放写入锁
	 */
	public void notifyWrite(boolean rewrite) {
		mRewrite = rewrite;
		synchronized (mLocker) {
			mLocker.notify();// 释放写入
		}
	}

	/**
	 * 写入设备数据
	 *
	 * @param data         写入的数据
	 * @param waitResponse 是否等待数据回调
	 * @return 是否写入成功
	 */
	public boolean writeData(byte[] data, boolean waitResponse) {
		if (mBluetoothSocket == null) {
			return false;
		}
		try {
			while (true) {
				OutputStream outputStream = mBluetoothSocket.getOutputStream();
				if (outputStream == null || !mBluetoothSocket.isConnected()) {
					return false;
				}
				outputStream.write(data);
				if (mBtManager != null) {
					mBtManager.onWriteData(this, data);
				}
				if (waitResponse) {
					try {
						long mWriteTime;
						synchronized (mLocker) {
							mWriteTime = System.currentTimeMillis();
							mLocker.wait(WRITE_TIMEOUT);// 3秒超时
						}
						// 写入超时
						if (System.currentTimeMillis() - mWriteTime >= WRITE_TIMEOUT) {
							return false;
						}
						if (mRewrite) {
							mRewrite = false;
							continue;
						}
						return true;
					} catch (InterruptedException e) {
						if (BuildConfig.DEBUG) {
							e.printStackTrace();
						}
						return false;
					}
				}
				return true;
			}
		} catch (IOException e) {
			if (BuildConfig.DEBUG) {
				e.printStackTrace();
			}
		}
		return false;
	}

	/**
	 * 读取数据
	 *
	 * @throws IOException 读取异常
	 */
	void readData() throws IOException {
		if (mBluetoothSocket == null) {
			return;
		}
		InputStream inputStream = mBluetoothSocket.getInputStream();
		if (inputStream == null || !mBluetoothSocket.isConnected()) {
			return;
		}
		byte[] buffer = new byte[1024];
		int count = inputStream.read(buffer);
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Read Data size:" + count);
		}
		byte[] data;
		if (count < 0) {
			data = new byte[0];
		} else {
			data = new byte[count];
			System.arraycopy(buffer, 0, data, 0, count);
		}
		if (mBtManager != null) {
			mBtManager.onReadData(this, data);
		}
	}

	/**
	 * 连接线程
	 */
	class ConnectThread extends Thread {
		@Override
		public void run() {
			super.run();
			if (BuildConfig.DEBUG) {
				Log.d(TAG, "Starting connect secure=" + mSecure);
			}
			mState = STATE_CONNECTING;
			try {
				if (mBluetoothSocket != null && mBluetoothSocket.isConnected()) {
					try {
						mBluetoothSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
				if (BuildConfig.DEBUG) {
					Log.d(TAG, "Creating BluetoothSocket");
				}
				if (mSecure) {
					mBluetoothSocket = mDevice.createRfcommSocketToServiceRecord(BtConfig.UUID_SECURE);
				} else {
					mBluetoothSocket = mDevice.createInsecureRfcommSocketToServiceRecord(BtConfig.UUID_INSECURE);
				}
				if (mBluetoothSocket == null) {
					mState = STATE_IDLE;
					mBtManager.onStateChanged(mState, BtDevice.this);
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Creating BluetoothSocket failed");
					}
					return;
				}
				// 链接
				try {
					mBtManager.cancelDiscovery();
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Connecting BluetoothSocket");
					}
					// 这是一个阻塞调用,只返回一个成功连接上或发生异常
					mBluetoothSocket.connect();
					if (BuildConfig.DEBUG) {
						Log.d(TAG, "Connected BluetoothSocket success");
					}
					mState = STATE_CONNECTED;
					mBtManager.onStateChanged(mState, BtDevice.this);
					startWorking();
					return;
				} catch (IOException e) {
					mState = STATE_IDLE;
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					try {
						if (mBluetoothSocket != null) {
							mBluetoothSocket.close();
						}
					} catch (IOException e2) {
						if (BuildConfig.DEBUG) {
							Log.e(TAG, "unable to close() mSecure=" + mSecure + " socket during connection failure", e2);
						}
					}
				}

			} catch (IOException e) {
				mState = STATE_IDLE;
				if (BuildConfig.DEBUG) {
					Log.e(TAG, "Socket Type: mSecure=" + mSecure + ",create() failed", e);
				}
			}
			mBtManager.onStateChanged(mState, BtDevice.this);
		}
	}

	/**
	 * 读取线程
	 */
	class ReadThread extends Thread {
		@Override
		public void run() {
			super.run();
			while (mReading && mState == STATE_CONNECTED) {
				try {
					readData();
				} catch (IOException e) {
					if (BuildConfig.DEBUG) {
						e.printStackTrace();
					}
					close();
					return;
				}
			}
		}
	}

	/**
	 * 连接设备
	 *
	 * @param secure 是否加密
	 * @return 是否连接成功
	 */
	boolean connect(boolean secure) {
		if (mState == STATE_CONNECTED || mState == STATE_CONNECTING) {
			return true;
		}
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Do connect secure=" + secure);
		}
		mState = STATE_CONNECTING;
		mSecure = secure;
		if (mConnectThread != null) {
			mConnectThread.interrupt();
		}
		mConnectThread = new ConnectThread();
		mConnectThread.start();
		return true;
	}

	/**
	 * 获取蓝牙设备
	 *
	 * @return 系统蓝牙设备对象
	 */
	public BluetoothDevice getBluetoothDevice() {
		return mDevice;
	}

	/**
	 * 关闭设备
	 */
	void closeInner() {
		mReading = false;
		mState = STATE_IDLE;
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "BtDevice close");
		}
		if (mBluetoothSocket != null) {
			try {
				mBluetoothSocket.close();
				mBluetoothSocket = null;
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		if (mConnectThread != null) {
			mConnectThread.interrupt();
		}
		if (mReadThread != null) {
			mReadThread.interrupt();
		}
		if (mBtManager != null) {
			mBtManager.onDisconnected(this);
		}
	}

	/**
	 * 关闭设备
	 */
	public void close() {
		if (mState == STATE_IDLE) {
			return;
		}
		mBtManager.close(this);
	}

	/**
	 * 设备服务端连接
	 *
	 * @param bluetoothSocket 服务端连接对象
	 */
	public void setBluetoothSocket(BluetoothSocket bluetoothSocket) {
		if (bluetoothSocket == null) {
			return;
		}
		close();
		mBluetoothSocket = bluetoothSocket;
		if (mBluetoothSocket.isConnected()) {
			mState = STATE_CONNECTED;
		}
		startWorking();
	}

	/**
	 * 开始读取已连接的设备
	 */
	private void startWorking() {
		if (mBluetoothSocket == null || mState != STATE_CONNECTED) {
			return;
		}
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "startWorking");
		}
		if (mReadThread != null) {
			mReading = false;
			mReadThread.interrupt();
		}
		mReadThread = new ReadThread();
		mReading = true;
		mReadThread.start();
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "Start Reading");
		}
	}

	@Override
	public boolean equals(Object o) {
		if (o instanceof BluetoothDevice) {
			return o.equals(mDevice);
		} else if (o instanceof BtDevice) {
			return ((BtDevice) o).mDevice.equals(mDevice);
		}
		return super.equals(o);
	}

	/**
	 * 发送一个蓝牙包
	 *
	 * @param data        需要发送的数据
	 * @param packetDelay 每一个包之间的时间间隔
	 * @return 是否发送成功
	 */
	public boolean sendOnePacket(byte[] data, int packetDelay) {
		return sendOnePacket(data, packetDelay, false);
	}

	/**
	 * 发送一个蓝牙包
	 *
	 * @param data        需要发送的数据
	 * @param packetDelay 每一个包之间的时间间隔
	 * @return 是否发送成功
	 */
	public boolean sendOnePacket(byte[] data, int packetDelay, boolean waitResponse) {
		//每次只能发送20个字节的包
		if (data.length > 20) {
			return false;
		}
		//检查连接状态
		if (!isConnected()) {
			return false;
		}
		if (BuildConfig.DEBUG) {
			Log.d(TAG, "BtDevice sendOneSppPacket");
		}
		boolean result = writeData(data, waitResponse);
		printData(data);
		if (!result) {
			return false;
		}
		if (packetDelay > 0) {
			try {
				Thread.sleep(packetDelay);//取决于串口的速度,不能小于波特率9600发送的长度
			} catch (InterruptedException e) {
				if (BuildConfig.DEBUG) {
					e.printStackTrace();
				}
			}
		}
		return true;
	}

	/**
	 * 打印数据
	 *
	 * @param data 打印的数据
	 */
	private void printData(byte[] data) {
		if (BuildConfig.DEBUG) {
			String hexData = "";
			for (int i = 0; i < data.length; i++) {
				hexData += String.format("0x%02x,", data[i]);
			}
			Log.d("DataSend:", "data[" + data.length + "]:\t" + hexData);
		}
	}

}
