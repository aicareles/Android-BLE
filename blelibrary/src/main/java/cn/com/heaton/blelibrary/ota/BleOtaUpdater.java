package cn.com.heaton.blelibrary.ota;

import android.os.Handler;
import android.util.Log;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.concurrent.Semaphore;

import cn.com.heaton.blelibrary.ble.BleConfig;
import cn.com.heaton.blelibrary.ble.BleManager;
import cn.com.heaton.blelibrary.ble.BleDevice;
import cn.com.heaton.blelibrary.BuildConfig;

/**
 * OTA Update Manager
 * Created by LiuLei on 2016/5/17.
 */
public class BleOtaUpdater implements OtaListener {
	private static final String TAG = BleOtaUpdater.class.getSimpleName();
	private BleDevice mBleDevice;
	private BleManager mBleManager;
	private       int       mStartOffset = 0;//The file reads the position offset
	private       int       mPercent     = 0;//File read percentage
	private final int       mTimeout     = 12;//Write timeout (seconds)
	private final int       mPacketSize  = 256;//Packet size
	private       boolean   mShouldStop  = false;//Whether to stop
	private Handler mHandler;//Main thread object
	private String mFilePath    = null;//file path
	private int    mByteRate    = 0;//Transmission rate
	private int    mElapsedTime = 0;//Every time the package is time consuming
	private Thread                       mUpdateThread;
	private Semaphore                    semp;//Send lock
	private OtaStatus.OtaResult mRetValue;//return value
	private Runnable                     mUpdateRunnable;//Execute thread
	private int mIndex = 0;//Execute the index

	public static final int OTA_UPDATE = 1;
	public static final int OTA_OVER   = 2;
	public static final int OTA_FAIL   = 3;
	public static final int OTA_BEFORE = 4;

	public BleOtaUpdater(Handler handler) {
		mHandler = handler;
		this.mRetValue = OtaStatus.OtaResult.OTA_RESULT_SUCCESS;
		// Initialize the thread
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

	private byte cmdToValue(OtaStatus.OtaCmd cmd) {
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

	private OtaStatus.OtaCmd valueToCmd(int val) {
		switch (val & 255) {
			case 1:
				return OtaStatus.OtaCmd.OTA_CMD_META_DATA;
			case 2:
				return OtaStatus.OtaCmd.OTA_CMD_BRICK_DATA;
			case 3:
				return OtaStatus.OtaCmd.OTA_CMD_DATA_VERIFY;
			case 4:
				return OtaStatus.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE;
			default:
				return null;
		}
	}

	/**
	 * send data
	 * @param data data
	 * @return Whether to write success
	 * @throws InterruptedException
	 */
	private boolean otaWrite(byte[] data) throws InterruptedException {
		// Whether to stop updating
		if (this.shouldStopUpdate()) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "otaWrite:Stopped for some reason");
			}
			return false;
		} else if (!this.mBleManager.getBleService().writeOtaData(mBleDevice.getBleAddress(),data)) {//Whether to write failed
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "Failed to write characteristic");
			}
			return false;
		} else {
			return this.waitWriteDataCompleted();//Write done
		}
	}

	/**
	 * Send a packet
	 * @param cmd Send the command
	 * @param checksum Check the value
	 * @param data data
	 * @param dataLength Data length
	 * @return Whether to send successfully
	 */
	private boolean otaSendPacket(OtaStatus.OtaCmd cmd, short checksum, byte[] data, int dataLength) {
		// Get the command byte
		byte cmdVal = this.cmdToValue(cmd);
		// Conversion check length is byte (high and low)
		byte[] checksumBytes = new byte[]{(byte) checksum, (byte) (checksum >> 8)};
		// Initialize the send header
		byte[] head = new byte[3];
		int packetLength;// Send packet size
		byte[] dataPacket; // Sent data
		// Package to send data
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
		// Traverse and send packets at a maximum of 20 bytes each time
		for (byte BytesEachTime = 20; left > 0; left -= tempLen) {
			if (left > BytesEachTime) {
				tempLen = BytesEachTime;
			} else {
				tempLen = left;
			}

			byte[] tempPacket = new byte[tempLen];
			System.arraycopy(dataPacket, packetLength - left, tempPacket, 0, tempLen);

			try {
				// Start sending data
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
	 * Send meta information data
	 * @param fin Data read in stream
	 * @return The size of the send
	 * @throws IOException
	 */
	private int otaSendMetaData(FileInputStream fin) throws IOException {
		// Read 2 bytes in the file
		byte[] metaLen = new byte[2];
		fin.read(metaLen);
		// Converted to total data length (high and low)
		short dataLength = (short) (((metaLen[1] & 255) << 8) + (metaLen[0] & 255));
		byte[] data = new byte[dataLength];
		// Read metadata
		int readLength = fin.read(data);
		if (readLength < 0) {
			return -1;
		} else {
			// Gets the check size of each byte of Meta
			short checksum = this.cmdToValue(OtaStatus.OtaCmd.OTA_CMD_META_DATA);

			// Statistics the size of the entire meta check
			for (int i = 0; i < readLength; ++i) {
				checksum = (short) (checksum + (data[i] & 255));
			}
			return this.otaSendPacket(OtaStatus.OtaCmd.OTA_CMD_META_DATA, checksum, data, dataLength) ? readLength + 2 : -1;
		}
	}

	/**
	 * Send data block
	 * @param fin File read stream
	 * @param dataLength Data block size
	 * @return Send size
	 * @throws IOException
	 */
	private int otaSendBrickData(FileInputStream fin, int dataLength) throws IOException {
		byte[] data = new byte[dataLength];
		// Read the data block
		int readLength = fin.read(data);
		if (readLength <= 0) {// Reading failed
			if(BuildConfig.DEBUG) {
				Log.w(TAG, "otaSendBrickData:No data read from file");
			}
			return -1;
		} else {
			if (readLength < dataLength) {
				dataLength = readLength;
			}
			// Get the size of each byte
			short checksum = this.cmdToValue(OtaStatus.OtaCmd.OTA_CMD_BRICK_DATA);

			// The size of the checksum of the statistics block
			for (int i = 0; i < dataLength; ++i) {
				checksum = (short) (checksum + (data[i] & 255));
			}

			// Send a packet
			if (this.otaSendPacket(OtaStatus.OtaCmd.OTA_CMD_BRICK_DATA, checksum, data, dataLength)) {
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
	 * Send the authentication packet command
	 * @return whether succeed
	 */
	private boolean otaSendVerifyCmd() {
		byte checksum = this.cmdToValue(OtaStatus.OtaCmd.OTA_CMD_DATA_VERIFY);
		return this.otaSendPacket(OtaStatus.OtaCmd.OTA_CMD_DATA_VERIFY, checksum, null, 0) && this.waitVerifyCmdDone();
	}

	/**
	 * Send a reset service command
	 */
	private void otaSendResetCmd() {
		byte checksum = this.cmdToValue(OtaStatus.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE);
		this.otaSendPacket(OtaStatus.OtaCmd.OTA_CMD_EXECUTION_NEW_CODE, checksum, null, 0);
	}

	private void releaseSemaphore(Semaphore semp) {
		semp.release();
	}

	/**
	 * Wait for the lock to release
	 * @param semp Lock object
	 * @return Whether it is overtime
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
		} while (!this.shouldStopUpdate());//Has stopped updating

		return false;
	}

	/**
	 * Set the file offset
	 * @param offset offset
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
	 * Wait for the lock to release
	 * @return Whether the lock was released successfully
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
	 * Get update results
	 * @param notify_data Result object
	 */
	public void otaGetResult(byte[] notify_data) {
		OtaStatus.OtaCmd cmdType = this.valueToCmd(notify_data[2] & 255);
		if (cmdType == null) {
			this.otaPrintBytes(notify_data, "Notify data: ");
			this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_RECEIVED_INVALID_PACKET);
		} else {
			switch (notify_data[3]) {
				case 0:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_SUCCESS);
					break;
				case 1:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_PKT_CHECKSUM_ERROR);
					break;
				case 2:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_PKT_LEN_ERROR);
					break;
				case 3:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_DEVICE_NOT_SUPPORT_OTA);
					break;
				case 4:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_FW_SIZE_ERROR);
					break;
				case 5:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_FW_VERIFY_ERROR);
					break;
				default:
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_INVALID_ARGUMENT);
			}

			if (this.mRetValue != OtaStatus.OtaResult.OTA_RESULT_SUCCESS) {
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
						this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_INVALID_ARGUMENT);
				}

			}
		}
	}

	/**
	 * Has stopped updating
	 * @return Whether to stop updating
	 */
	private boolean shouldStopUpdate() {
		return this.mShouldStop;
	}

	private void serErrorCode(OtaStatus.OtaResult ret) {
		this.mRetValue = ret;
	}

	/**
	 * Start Update
	 * @param file file path
	 * @param bleDevice Device object
	 * @param bleManager Device management class
	 * @return Update the results
	 */
	public OtaStatus.OtaResult otaStart(String file,BleDevice bleDevice, BleManager bleManager) {
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
			// Update thread
			mUpdateThread = new Thread(this.mUpdateRunnable);
			// Start Update
			mUpdateThread.start();
			return OtaStatus.OtaResult.OTA_RESULT_SUCCESS;
		} else {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "otaUpdateInit:argument invalid");
			}
			return OtaStatus.OtaResult.OTA_RESULT_INVALID_ARGUMENT;
		}
	}

	/**
	 * Start Update
	 * @param filePath file path
	 */
	private void otaUpdateProcess(String filePath) {
		try {
			// Send starts to update the callback message
			if (mHandler != null) {
				mHandler.obtainMessage(OTA_BEFORE, mIndex).sendToTarget();
			}
			// Get the file into the stream
			FileInputStream fileInputStream = new FileInputStream(filePath);
			// Get the file size
			int fileSize = fileInputStream.available();
			if (fileSize == 0 || mShouldStop) {// The file size is 0, ending the update
				fileInputStream.close();
				this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_FW_SIZE_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// Send meta information data
			int metaSize = this.otaSendMetaData(fileInputStream);
			if (metaSize < 0 || mShouldStop) {// Sending metadata error
				fileInputStream.close();
				this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_SEND_META_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// Gets the currently sent offset
			int offset1 = this.getOffset();
			if (offset1 < 0 || mShouldStop) {// The offset is incorrect
				if(BuildConfig.DEBUG) {
					Log.e(TAG, "wait cmd OTA_CMD_META_DATA timeout");
				}
				fileInputStream.close();
				this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_META_RESPONSE_TIMEOUT);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// Offset is greater than 0
			if (offset1 > 0) {//Excessive sent offset
				fileInputStream.skip((long) offset1);
			}

			// Remaining data block size
			int brickDataSize = fileSize - metaSize;
			int transfereedSize = 0;
			if(BuildConfig.DEBUG) {
				Log.d(TAG, "offset=" + offset1 + " meta size " + metaSize);
			}
			long begin = Calendar.getInstance().getTimeInMillis();

			do {// Traverse the read file every time you send mPacketSize bytes
				// Send data block
				int ret1 = this.otaSendBrickData(fileInputStream, mPacketSize);
				// The sending data block is wrong
				if (ret1 < 0 || mShouldStop) {
					fileInputStream.close();
					if(BuildConfig.DEBUG) {
						Log.e(TAG, "otaUpdateProcess Exit for some transfer issue");
					}
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
					if (mHandler != null) {
						mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
					}
					return;
				}

				// Wait for data to read the lock
				if (!this.waitReadDataCompleted() || mShouldStop) {// An error occurred
					if(BuildConfig.DEBUG) {
						Log.e(TAG, "waitReadDataCompleted timeout");
					}
					this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
					if (mHandler != null) {
						mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
					}
					return;
				}

				// Increase the offset of data sent
				offset1 += ret1;
				// Record the percentage of sent
				this.mPercent = offset1 * 100 / fileSize;

				if (mHandler != null) {
					mHandler.obtainMessage(OTA_UPDATE, mPercent, 0, mIndex).sendToTarget();
				}

				// Record the sent data block
				transfereedSize += mPacketSize;
				long now = Calendar.getInstance().getTimeInMillis();
				// Record the total data block transmission time
				this.mElapsedTime = (int) ((now - begin) / 1000L);
				// Calculate the transfer rate
				this.mByteRate = (int) ((long) (transfereedSize * 1000) / (now - begin));
			} while (offset1 < brickDataSize);

			// Send the server to verify the packet command
			if (!this.otaSendVerifyCmd() || mShouldStop) {
				fileInputStream.close();
				this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_FW_VERIFY_ERROR);
				if (mHandler != null) {
					mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
				}
				return;
			}

			// Write percentage 100
			this.mPercent = 100;
			// Send a reset service command
			this.otaSendResetCmd();
			fileInputStream.close();
		} catch (Exception e) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "send ota update error",e);
			}
			this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_DATA_RESPONSE_TIMEOUT);
			if (mHandler != null) {
				mHandler.obtainMessage(OTA_FAIL, mIndex).sendToTarget();
			}
			return;
		}

		if(BuildConfig.DEBUG) {
			Log.i(TAG, "otaUpdateProcess Exit");
		}
		// Send callback successful
		if (mHandler != null) {
			mHandler.obtainMessage(OTA_OVER, mIndex).sendToTarget();
		}
		this.serErrorCode(OtaStatus.OtaResult.OTA_RESULT_SUCCESS);
	}

	/**
	 * Get update progress
	 * @param extra Update progress
	 * @return Data results
	 */
	public OtaStatus.OtaResult otaGetProcess(int[] extra) {
		if (extra.length < 8) {
			if(BuildConfig.DEBUG) {
				Log.e(TAG, "buffer is too small,at least 8 intgent");
			}
			return OtaStatus.OtaResult.OTA_RESULT_INVALID_ARGUMENT;
		} else {
			Arrays.fill(extra, 0);
			extra[0] = this.mPercent;
			extra[1] = this.mByteRate;
			extra[2] = this.mElapsedTime;
			return this.mRetValue;
		}
	}

	/**
	 * Stop updating
	 */
	public void otaStop() {
		this.mShouldStop = true;
		if(mUpdateThread != null) {
			mUpdateThread.interrupt();
		}
	}
}
