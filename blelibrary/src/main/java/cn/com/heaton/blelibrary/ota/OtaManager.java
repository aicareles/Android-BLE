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
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

import cn.com.heaton.blelibrary.BleConfig;
import cn.com.heaton.blelibrary.BleManager;
import cn.com.heaton.blelibrary.BleVO.BleDevice;
import cn.com.heaton.blelibrary.BuildConfig;
import cn.com.heaton.blelibrary.R;

/**
 * OTA管理器
 * Created by DDL on 2016/3/8.
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
		private WeakReference<OtaManager> weakReference;

		public MessageHandler(OtaManager otaManager) {
			weakReference = new WeakReference<>(otaManager);
		}

		@Override
		public void dispatchMessage(Message msg) {
			final OtaManager otaManager = weakReference.get();
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
					//准备更新
					case BleOtaUpdater.OTA_BEFORE:
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onPreUpdate(bleDevice);
						}
						return;
					//更新中
					case BleOtaUpdater.OTA_UPDATE:
						if (otaManager.mStopUpdate) {
							return;
						}
						otaManager.showProgress(msg.arg1);
						if (otaManager.mUpdateListener != null) {
							otaManager.mUpdateListener.onUpdating(bleDevice, msg.arg1);
						}
						return;
					//更新完成【成功】
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
					//更新失败
					case BleOtaUpdater.OTA_FAIL:
						if (otaManager.mUpdateDialog != null) {
							otaManager.mUpdateDialog.dismiss();

							AlertDialog.Builder builder = new AlertDialog.Builder(otaManager.mContext);
							builder.setTitle("硬件更新");
							builder.setMessage(otaManager.mContext.getResources().getString(R.string.ota_error, bleDevice != null ? "【" + bleDevice.getmBleName() + "】" : ""));
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
	 * 设置是否单条ota更新
	 *
	 * @param single true为单条更新
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
	 * 重试ota更新
	 *
	 * @param otaUpdater 更新对象
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
	 * 开始ota更新
	 *
	 * @param file      更新文件
	 * @param bleManager 设备管理类
	 * @return 是否正确执行更新
	 */
	public boolean startOtaUpdate(File file, BleDevice bleDevice, BleManager bleManager) {
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
			if (mBleOtaUpdater.otaStart(mFilePath, bleDevice, bleManager) == BleConfig.OtaResult.OTA_RESULT_SUCCESS) {
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
	 * 停止所有ota更新
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
	 * 展示进度条，单条ota更新有效
	 *
	 * @param progress 进度
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
			mUpdateDialog.setTitle(R.string.ota_title);
			mUpdateDialog.setMessage("更新中，请稍后...");
			mUpdateDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);//设置进度条对话框//样式（水平，旋转）
//			mUpdateDialog.setMax(MAX_PROGRESS);
			mUpdateDialog.show();
			mUpdateDialog.setProgress(progress);

		}
		if (!mUpdateDialog.isShowing()) {
			mUpdateDialog.show();
		}
//		if (mProgress != null) {
//			mProgress.setProgress(progress);
//		}
	}
}
