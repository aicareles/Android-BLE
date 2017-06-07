package cn.com.heaton.blelibrary.ota;

import android.os.Handler;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Semaphore;

import cn.com.heaton.blelibrary.BleConfig;
import cn.com.heaton.blelibrary.BleManager;
import cn.com.heaton.blelibrary.BleVO.BleDevice;
import cn.com.heaton.blelibrary.BuildConfig;

/**
 * OTA更新管理器
 * Created by DDL on 2016/5/17.
 */
public class BleOtaUpdater implements OtaListener {
	private static final String TAG = BleOtaUpdater.class.getSimpleName();
	private BleDevice mBleDevice;
	private BleManager mBleManager;
	private       int       mStartOffset = 0;//文件读取位置偏移量
	private       int       mPercent     = 0;//文件读取百分比
	private final int       mTimeout     = 12;//写入超时时间（秒）
	private final int       mPacketSize  = 256;//数据包大小
	private       boolean   mShouldStop  = false;//是否停止
	private Handler mHandler;//主线程对象
	private String mFilePath    = null;//文件路径
	private int    mByteRate    = 0;//传输速率
	private int    mElapsedTime = 0;//每次发包耗时
	private Thread                       mUpdateThread;
	private Semaphore                    semp;//发送锁
	private BleConfig.OtaResult mRetValue;//返回值
	private Runnable                     mUpdateRunnable;//执行线程
	private int mIndex = 0;//执行索引

	public static final int OTA_UPDATE = 1;
	public static final int OTA_OVER   = 2;
	public static final int OTA_FAIL   = 3;
	public static final int OTA_BEFORE = 4;

	public BleOtaUpdater(Handler handler) {
		mHandler = handler;
		this.mRetValue = BleConfig.OtaResult.OTA_RESULT_SUCCESS;
		// 初始化线程
		this.mUpdateRunnable = new Runnable() {
			public void run() {
				BleOtaUpdater.this.otaUpdateProcess(BleOtaUpdater.this.mFilePath);
			}
		};
	}

	@Override
	public void onWrite() {
		notifyReadDataCompleted();
	}

	@Override
	public void onChange(byte[] data) {
		otaGetResult(data);
	}

	public void setIndex(int idx) {
		mIndex = idx;
	}

	public int getIndex() {
		return mIndex;
	}

	public BleDevice getBleDevice() {
		return mBleDevice;
	}

	public BleManager getBleManager(){
		return mBleManager;
	}

	public void otaPrintBytes(byte[] bytes, String tag) {
		if (bytes != null) {
			StringBuilder stringBuilder = new StringBuilder(bytes.length);

			for (byte byteChar : bytes) {
				stringBuilder.append(String.format("%02X ", byteChar));
			}
			if(BuildConfig.DEBUG) {
				Log.i(TAG, tag + " :" + stringBuilder.toString());
			}
		}
	}

	private byte cmdToValue(BleConfig.OtaCmd cmd) {
		switch (cmd) {
			case OTA_CMD_META_DATA:
				return (byte) 1;
			case OTA_CMD_BRICK_DATA:
				return (byte) 2;
			case OTA_CMD_DATA_VERIFY:
				return (byte) 3;
			case OTA_CMD_EXECUTION_NEW_CODE:
				return (byte) 4;
			default:
				return (byte) 0;
		}
	}

	private BleConfig.OtaCmd valueToCmd(int val) {
		switch (val & 255) {
			case 1:
				return BleConfig.OtaCmd.OTA_CMD_META_DATA;
			case 2:
				return BleConfig.OtaCmd.OTA_CMD_BRICK_DATA;
			case 3:
				return BleConfig.OtaCmd.OTA_CMD_DATA_VERIFY;
			case 4:
				return BleConfig.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE;
			default:
				return null;
		}
	}

	/**
	 * 发送数据
	 * @param data 数据
	 * @return 是否写入成功
	 * @throws InterruptedException
	 */
	private boolean otaWrite(byte[] data) throws InterruptedException {
		// 是否停止更新
		if (this.shouldStopUpdate()) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "otaWrite:Stopped for some reason");
			}
			return false;
		} else if (!this.mBleManager.getBleService().writeOtaData(mBleDevice.getBleAddress(),data)) {//是否写入失败
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "Failed to write characteristic");
			}
			return false;
		} else {
			return this.waitWriteDataCompleted();//写入完成
		}
	}

	/**
	 * 发送数据包
	 * @param cmd 发送的命令
	 * @param checksum 效验值
	 * @param data 数据
	 * @param dataLength 数据长度
	 * @return 是否发送成功
	 */
	private boolean otaSendPacket(BleConfig.OtaCmd cmd, short checksum, byte[] data, int dataLength) {
		// 获取命令字节
		byte cmdVal = this.cmdToValue(cmd);
		// 转换效验长度为字节 （高低位）
		byte[] checksumBytes = new byte[]{(byte) checksum, (byte) (checksum >> 8)};
		// 初始化发送头
		byte[] head = new byte[3];
		int packetLength;// 发送包大小
		byte[] dataPacket; // 发送的数据
		// 打包发送数据
		switch (cmd) {
			case OTA_CMD_META_DATA:
			case OTA_CMD_BRICK_DATA:
				head[0] = (byte) (dataLength + 1);
				head[1] = (byte) (dataLength + 1 >> 8);
				head[2] = cmdVal;
				packetLength = head.length + dataLength + checksumBytes.length;
				dataPacket = new byte[packetLength];
				System.arraycopy(head, 0, dataPacket, 0, head.length);
				System.arraycopy(data, 0, dataPacket, head.length, dataLength);
				System.arraycopy(checksumBytes, 0, dataPacket, head.length + dataLength, checksumBytes.length);
				break;
			case OTA_CMD_DATA_VERIFY:
			case OTA_CMD_EXECUTION_NEW_CODE:
				packetLength = head.length + checksumBytes.length;
				dataPacket = new byte[packetLength];
				dataPacket[0] = 1;
				dataPacket[1] = 0;
				dataPacket[2] = cmdVal;
				dataPacket[3] = checksumBytes[0];
				dataPacket[4] = checksumBytes[1];
				break;
			default:
				if(BuildConfig.DEBUG) {
					Log.e(TAG, "otaSendPacket:unknown cmd type");
				}
				return false;
		}

		int left = packetLength;

		int tempLen;
		// 遍历并发送数据包，每次最大20字节
		for (byte BytesEachTime = 20; left > 0; left -= tempLen) {
			if (left > BytesEachTime) {
				tempLen = BytesEachTime;
			} else {
				tempLen = left;
			}

			byte[] tempPacket = new byte[tempLen];
			System.arraycopy(dataPacket, packetLength - left, tempPacket, 0, tempLen);

			try {
				// 开始发送数据
				if (!this.otaWrite(tempPacket)) {
					return false;
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}

		return true;
	}

	/**
	 * 发送元信息数据
	 * @param fin 数据读入流
	 * @return 发送的大小
	 * @throws IOException
	 */
	private int otaSendMetaData(FileInputStream fin) throws IOException {
		// 读取文件中的2个字节
		byte[] metaLen = new byte[2];
		fin.read(metaLen);
		// 转化为数据总长度（高低位）
		short dataLength = (short) (((metaLen[1] & 255) << 8) + (metaLen[0] & 255));
		byte[] data = new byte[dataLength];
		// 读取元数据
		int readLength = fin.read(data);
		if (readLength < 0) {
			return -1;
		} else {
			// 获取Meta的每字节的效验大小
			short checksum = this.cmdToValue(BleConfig.OtaCmd.OTA_CMD_META_DATA);

			// 统计整个meta的效验大小
			for (int i = 0; i < readLength; ++i) {
				checksum = (short) (checksum + (data[i] & 255));
			}
			return this.otaSendPacket(BleConfig.OtaCmd.OTA_CMD_META_DATA, checksum, data, dataLength) ? readLength + 2 : -1;
		}
	}

	/**
	 * 发送数据块
	 * @param fin 文件读取流
	 * @param dataLength 数据块大小
	 * @return 发送大小
	 * @throws IOException
	 */
	private int otaSendBrickData(FileInputStream fin, int dataLength) throws IOException {
		byte[] data = new byte[dataLength];
		// 读取数据块
		int readLength = fin.read(data);
		if (readLength <= 0) {// 读取失败
			if(BuildConfig.DEBUG) {
				Log.w(TAG, "otaSendBrickData:No data read from file");
			}
			return -1;
		} else {
			if (readLength < dataLength) {
				dataLength = readLength;
			}
			// 获取每字节效验值大小
			short checksum = this.cmdToValue(BleConfig.OtaCmd.OTA_CMD_BRICK_DATA);

			// 统计数据块的效验值大小
			for (int i = 0; i < dataLength; ++i) {
				checksum = (short) (checksum + (data[i] & 255));
			}

			// 发送数据包
			if (this.otaSendPacket(BleConfig.OtaCmd.OTA_CMD_BRICK_DATA, checksum, data, dataLength)) {
				return readLength;
			} else {
				if(BuildConfig.DEBUG) {
					Log.e(TAG, "otaSendBrickData:failed to send packet");
				}
				return -2;
			}
		}
	}

	/**
	 * 发送验证数据包命令
	 * @return 是否成功
	 */
	private boolean otaSendVerifyCmd() {
		byte checksum = this.cmdToValue(BleConfig.OtaCmd.OTA_CMD_DATA_VERIFY);
		return this.otaSendPacket(BleConfig.OtaCmd.OTA_CMD_DATA_VERIFY, checksum, null, 0) && this.waitVerifyCmdDone();
	}

	/**
	 * 发送重置服务命令
	 */
	private void otaSendResetCmd() {
		byte checksum = this.cmdToValue(BleConfig.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE);
		this.otaSendPacket(BleConfig.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE, checksum, null, 0);
	}

	private void releaseSemaphore(Semaphore semp) {
		semp.release();
	}

	/**
	 * 等待锁释放
	 * @param semp 锁对象
	 * @return 是否超时
	 */
	private boolean waitSemaphore(Semaphore semp) {
		int i = 0;

		do {
			if (i++ >= mTimeout * 1000) {
				return false;
			}

			boolean getAccquire = semp.tryAcquire();
			if (getAccquire) {
				return true;
			}

			try {
				Thread.sleep(1L);
			} catch (InterruptedException e) {
				if(BuildConfig.DEBUG) {
					e.printStackTrace();
				}
				return true;
			}
		} while (!this.shouldStopUpdate());//是否已停止更新

		return false;
	}

	/**
	 * 设置文件偏移量
	 * @param offset 偏移量
	 */
	private void setOffset(int offset) {
		this.mStartOffset = offset;
		this.releaseSemaphore(this.semp);
	}

	private int getOffset() {
		return this.waitSemaphore(this.semp) ? this.mStartOffset : -1;
	}

	private void notifyVerifyCmdDone() {
		this.releaseSemaphore(this.semp);
	}

	private boolean waitVerifyCmdDone() {
		return this.waitSemaphore(this.semp);
	}

	public void notifyWriteDataCompleted() {
		this.releaseSemaphore(this.semp);
	}

	/**
	 * 等待锁释放
	 * @return 锁是否释放成功
	 */
	private boolean waitWriteDataCompleted() {
		return this.waitSemaphore(this.semp);
	}

	private void notifyReadDataCompleted() {
		this.releaseSemaphore(this.semp);
	}

	private boolean waitReadDataCompleted() {
		return this.waitSemaphore(this.semp);
	}

	/**
	 * 获取更新结果
	 * @param notify_data 结果对象
	 */
	public void otaGetResult(byte[] notify_data) {
		BleConfig.OtaCmd cmdType = this.valueToCmd(notify_data[2] & 255);
		if (cmdType == null) {
			this.otaPrintBytes(notify_data, "Notify data: ");
			this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_RECEIVED_INVALID_PACKET);
		} else {
			switch (notify_data[3]) {
				case 0:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_SUCCESS);
					break;
				case 1:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_PKT_CHECKSUM_ERROR);
					break;
				case 2:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_PKT_LEN_ERROR);
					break;
				case 3:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_DEVICE_NOT_SUPPORT_OTA);
					break;
				case 4:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_FW_SIZE_ERROR);
					break;
				case 5:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_FW_VERIFY_ERROR);
					break;
				default:
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_INVALID_ARGUMENT);
			}

			if (this.mRetValue != BleConfig.OtaResult.OTA_RESULT_SUCCESS) {
				this.otaPrintBytes(notify_data, "Notify data: ");
			} else {
				switch (cmdType) {
					case OTA_CMD_META_DATA:
						short offset = (short) ((notify_data[4] & 255) + ((notify_data[5] & 255) << 8));
						this.setOffset(offset);
						break;
					case OTA_CMD_BRICK_DATA:
						short size = (short) ((notify_data[4] & 255) + ((notify_data[5] & 255) << 8));
						this.notifyReadDataCompleted();
						break;
					case OTA_CMD_DATA_VERIFY:
						this.notifyVerifyCmdDone();
						if(BuildConfig.DEBUG) {
							Log.i(TAG, "OTA_CMD_DATA_VERIFY");
						}
						break;
					case OTA_CMD_EXECUTION_NEW_CODE:
						if(BuildConfig.DEBUG) {
							Log.i(TAG, "This should never happened");
						}
						break;
					default:
						if(BuildConfig.DEBUG) {
							Log.i(TAG, "Exit " + (notify_data[2] & 255));
						}
						this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_INVALID_ARGUMENT);
				}

			}
		}
	}

	/**
	 * 是否已停止更新
	 * @return 是否停止更新
	 */
	private boolean shouldStopUpdate() {
		return this.mShouldStop;
	}

	private void serErrorCode(BleConfig.OtaResult ret) {
		this.mRetValue = ret;
	}

	/**
	 * 开始更新
	 * @param file 文件路径
	 * @param bleDevice 设备对象
	 * @param bleManager 设备管理类
	 * @return 更新结果
	 */
	public BleConfig.OtaResult otaStart(String file,BleDevice bleDevice, BleManager bleManager) {
		if (!file.isEmpty() && bleManager != null) {
			this.mFilePath = file;
			this.mBleDevice = bleDevice;
			this.mBleManager = bleManager;
			this.mBleManager.getBleService().setOtaListener(this);
			this.mShouldStop = false;
			this.mPercent = 0;
			this.mByteRate = 0;
			this.mElapsedTime = 0;
			this.semp = new Semaphore(0);
			// 更新线程
			mUpdateThread = new Thread(this.mUpdateRunnable);
			// 开始更新
			mUpdateThread.start();
			return BleConfig.OtaResult.OTA_RESULT_SUCCESS;
		} else {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "otaUpdateInit:argument invalid");
			}
			return BleConfig.OtaResult.OTA_RESULT_INVALID_ARGUMENT;
		}
	}

	/**
	 * 开始更新
	 * @param filePath 文件路径
	 */
	private void otaUpdateProcess(String filePath) {
		try {
			// 发送开始更新回调消息
			if (mHandler != null) {
				mHandler.obtainMessage(OTA_BEFORE, mIndex).sendToTarget();
			}
			// 获取文件读入流
			FileInputStream fileInputStream = new FileInputStream(filePath);
			// 获取文件大小
			int fileSize = fileInputStream.available();
			if (fileSize == 0 || mShouldStop) {// 文件大小为0，结束更新
				fileInputStream.close();
				this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_FW_SIZE_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// 发送元信息数据
			int metaSize = this.otaSendMetaData(fileInputStream);
			if (metaSize < 0 || mShouldStop) {// 发送元数据出错
				fileInputStream.close();
				this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_SEND_META_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// 获取当前发送的偏移量
			int offset1 = this.getOffset();
			if (offset1 < 0 || mShouldStop) {// 偏移量有误
				if(BuildConfig.DEBUG) {
					Log.e(TAG, "wait cmd OTA_CMD_META_DATA timeout");
				}
				fileInputStream.close();
				this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_META_RESPONSE_TIMEOUT);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// 偏移量大于0
			if (offset1 > 0) {//过度已发送的偏移量
				fileInputStream.skip((long) offset1);
			}

			// 剩余数据块大小
			int brickDataSize = fileSize - metaSize;
			int transfereedSize = 0;
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "offset=" + offset1 + " meta size " + metaSize);
			}
			long begin = Calendar.getInstance().getTimeInMillis();

			do {// 遍历读取文件 每次发送 mPacketSize 个字节
				// 发送数据块
				int ret1 = this.otaSendBrickData(fileInputStream, mPacketSize);
				// 发送数据块有错
				if (ret1 < 0 || mShouldStop) {
					fileInputStream.close();
					if(BuildConfig.DEBUG) {
						Log.e(TAG, "otaUpdateProcess Exit for some transfer issue");
					}
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
					if (mHandler != null) {
						mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
					}
					return;
				}

				// 等待数据读取锁
				if (!this.waitReadDataCompleted() || mShouldStop) {// 出现错误
					if(BuildConfig.DEBUG) {
						Log.e(TAG, "waitReadDataCompleted timeout");
					}
					this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
					if (mHandler != null) {
						mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
					}
					return;
				}

				// 增加数据已发送的偏移量
				offset1 += ret1;
				// 记录已发送的百分比
				this.mPercent = offset1 * 100 / fileSize;

				if (mHandler != null) {
					mHandler.obtainMessage(OTA_UPDATE, mPercent, 0, mIndex).sendToTarget();
				}

				// 记录已发送的数据块
				transfereedSize += mPacketSize;
				long now = Calendar.getInstance().getTimeInMillis();
				// 记录总数据块发送时间
				this.mElapsedTime = (int) ((now - begin) / 1000L);
				// 计算传输速率
				this.mByteRate = (int) ((long) (transfereedSize * 1000) / (now - begin));
			} while (offset1 < brickDataSize);

			// 发送服务端验证数据包命令
			if (!this.otaSendVerifyCmd() || mShouldStop) {
				fileInputStream.close();
				this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_FW_VERIFY_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// 写入百分比100
			this.mPercent = 100;
			// 发送重置服务命令
			this.otaSendResetCmd();
			fileInputStream.close();
		} catch (Exception e) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "send ota update error",e);
			}
			this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
			if (mHandler != null) {
				mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
			}
			return;
		}

		if(BuildConfig.DEBUG) {
			Log.i(TAG, "otaUpdateProcess Exit");
		}
		// 发送回调成功
		if (mHandler != null) {
			mHandler.obtainMessage(OTA_OVER, mIndex).sendToTarget();
		}
		this.serErrorCode(BleConfig.OtaResult.OTA_RESULT_SUCCESS);
	}

	/**
	 * 获取更新进度
	 * @param extra 更新进度
	 * @return 数据结果
	 */
	public BleConfig.OtaResult otaGetProcess(int[] extra) {
		if (extra.length < 8) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "buffer is too small,at least 8 intgent");
			}
			return BleConfig.OtaResult.OTA_RESULT_INVALID_ARGUMENT;
		} else {
			Arrays.fill(extra, 0);
			extra[0] = this.mPercent;
			extra[1] = this.mByteRate;
			extra[2] = this.mElapsedTime;
			return this.mRetValue;
		}
	}

	/**
	 * 停止更新
	 */
	public void otaStop() {
		this.mShouldStop = true;
		if(mUpdateThread != null) {
			mUpdateThread.interrupt();
		}
	}
}
