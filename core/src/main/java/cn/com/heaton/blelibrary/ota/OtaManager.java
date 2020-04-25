package cn.com.heaton.blelibrary.ota;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.widget.ProgressBar;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.BuildConfig;
import cn.com.heaton.blelibrary.R;

/**
 * OTA  Manager
 * Created by LiuLei on 2016/3/8.
 */
public class OtaManager {

	public static final String TAG = "OtaManager";

	public interface UpdateListener {
		void onPreUpdate(BleDevice bleDevice);

		void onUpdating(BleDevice bleDevice, int progress);

		void onUpdateComplete(BleDevice bleDevice);

		void onUpdateFailed(BleDevice bleDevice);
	}

	private static class MessageHandler extends Handler {
//		private WeakReference<OtaManager> weakReference;
		private OtaManager otaManager;

		public MessageHandler(OtaManager otaManager) {
//			weakReference = new WeakReference<>(otaManager);
			this.otaManager = otaManager;
		}

		@Override
		public void dispatchMessage(Message msg) {
//			final OtaManager otaManager = weakReference.get();
			if (otaManager != null) {
				if (BuildConfig.DEBUG) {
					Log.i(TAG, "dispatchMessage:" + msg.what);
				}
				Integer idx = (Integer) msg.obj;
				BleOtaUpdater updater = null;
				BleDevice bleDevice = null;
				if (idx != null) {
					updater = otaManager.mUpdateList.get(idx);
					if (updater != null) {
						bleDevice = updater.getBleDevice();
					}
				}
				switch (msg.what) {
					//Ready to update
					case BleOtaUpdater.OTA_BEFORE:
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onPreUpdate(bleDevice);
						}
						return;
					//updating
					case BleOtaUpdater.OTA_UPDATE:
						if (otaManager.mStopUpdate) {
							return;
						}
						otaManager.showProgress(msg.arg1);
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onUpdating(bleDevice, msg.arg1);
						}
						return;
					//Update complete
					case BleOtaUpdater.OTA_OVER:
						if (otaManager.mUpdateDialog != null) {
							otaManager.mUpdateDialog.dismiss();
						}
						if (updater != null) {
							otaManager.mUpdateList.remove(updater.getIndex());
						}
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onUpdateComplete(bleDevice);
						}
						return;
					//Update failed
					case BleOtaUpdater.OTA_FAIL:
						if (otaManager.mUpdateDialog != null) {
							otaManager.mUpdateDialog.dismiss();

							AlertDialog.Builder builder = new AlertDialog.Builder(otaManager.mContext);
							builder.setTitle("硬件更新");
							builder.setMessage(otaManager.mContext.getResources().getString(R.string.ota_error, bleDevice != null ? "[" + bleDevice.getBleName() + "]" : ""));
							final BleOtaUpdater otaUpdater = updater;
							builder.setPositiveButton(R.string.update_retry, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
									otaManager.retryUpdate(otaUpdater);
								}
							});
							builder.setNegativeButton(R.string.update_cancel, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									dialog.dismiss();
								}
							});
							Dialog noticeDialog = builder.create();
							noticeDialog.show();
						}
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onUpdateFailed(bleDevice);
						}
						return;
					default:
						super.dispatchMessage(msg);
				}
			}
		}
	}

	private Context mContext;
	private MessageHandler mHandler = new MessageHandler(this);
	private File           mOtaFile;
//	private AlertDialog    mUpdateDialog;
	private ProgressDialog mUpdateDialog;
	private ProgressBar    mProgress;
	private UpdateListener mUpdateListener;
	private SparseArray<BleOtaUpdater> mUpdateList = new SparseArray<>();
	private int                        mCounter    = 0;
	private boolean                    mSingle     = true;
	private boolean                    mStopUpdate = false;

	public OtaManager(Context context) {
		mContext = context;
	}

	/**
	 * Set whether or not a single ota is updated
	 *
	 * @param single True for a single update
	 */
	public void setSingle(boolean single) {
		mSingle = single;
	}

	public UpdateListener getUpdateListener() {
		return mUpdateListener;
	}

	public void setUpdateListener(UpdateListener updateListener) {
		this.mUpdateListener = updateListener;
	}

	/**
	 * Retry ota update
	 *
	 * @param otaUpdater Update the object
	 */
	private void retryUpdate(BleOtaUpdater otaUpdater) {
		if (mStopUpdate) {
			return;
		}
		if (otaUpdater == null) {
			return;
		}
		mUpdateList.remove(otaUpdater.getIndex());
		startOtaUpdate(mOtaFile, otaUpdater.getBleDevice(),otaUpdater.getBleManager());
	}

	/**
	 * Start ota update
	 *
	 * @param file      Update the file
	 * @param bleManager Device management class
	 * @return Whether the update is performed correctly
	 */
	public boolean startOtaUpdate(File file, BleDevice bleDevice, Ble bleManager) {
		if (bleManager == null || file == null || !file.exists() || !file.canRead()) {
			return false;
		}
		if (mSingle && mUpdateList.size() > 0) {
			return false;
		}
		mCounter++;
		BleOtaUpdater mBleOtaUpdater = new BleOtaUpdater(mHandler);
		mBleOtaUpdater.setIndex(mCounter);
		mUpdateList.put(mCounter, mBleOtaUpdater);
		try {
			mOtaFile = file;
			String mFilePath = file.getCanonicalPath();
			mStopUpdate = false;
			if (mBleOtaUpdater.otaStart(mFilePath, bleDevice, bleManager) == OtaStatus.OtaResult.OTA_RESULT_SUCCESS) {
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
	 * Stop all ota updates
	 */
	public void stopAll() {
		if (mStopUpdate) {
			return;
		}
		mStopUpdate = true;
		if (mUpdateList.size() > 0) {
			for (int i = 0, len = mUpdateList.size(); i < len; i++) {
				int key = mUpdateList.keyAt(i);
				BleOtaUpdater otaUpdater = mUpdateList.get(key);
				if (otaUpdater != null) {
					otaUpdater.otaStop();
				}
			}
		}
	}

	/**
	 * Show progress bar, single ota update valid
	 *
	 * @param progress progress
	 */
	private void showProgress(int progress) {
		if (!mSingle) {
			return;
		}
		if (mUpdateDialog == null) {
//			AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
//			builder.setTitle(R.string.ota_title);
//			try {
//				View view = LayoutInflater.from(mContext).inflate(AppConfig.OTA_DIALOG_LAYOUT, null);
//				builder.setView(view);
//				builder.setNegativeButton(R.string.update_cancel, new DialogInterface.OnClickListener() {
//					@Override
//					public void onClick(DialogInterface dialog, int which) {
//						dialog.dismiss();
//						stopAll();
//					}
//				});
//				mUpdateDialog = builder.create();
//				View progressView = view.findViewById(R.id.progress);
//				if (progressView != null && progressView instanceof ProgressBar) {
//					mProgress = (ProgressBar) progressView;
//				}
//				View v = view.findViewById(R.id.tv_text);
//				if (v != null && v instanceof TextView) {
//					((TextView) v).setText(R.string.updating);
//				}
//			} catch (Exception e) {
//				if (BuildConfig.DEBUG) {
//					e.printStackTrace();
//				}
//				return;
//			}
			mUpdateDialog = new ProgressDialog(mContext);
			mUpdateDialog.setTitle(R.string.update_cancel);
			mUpdateDialog.setMessage(mContext.getString(R.string.updating));
			mUpdateDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//Set the progress bar dialog box style (horizontal, rotate)
//			mUpdateDialog.setMax(MAX_PROGRESS);
		}
		if (!mUpdateDialog.isShowing()) {
			mUpdateDialog.show();
		}
		mUpdateDialog.setProgress(progress);
//		if (mProgress != null) {
//			mProgress.setProgress(progress);
//		}
	}
}
