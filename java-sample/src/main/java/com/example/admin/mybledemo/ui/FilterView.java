package com.example.admin.mybledemo.ui;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.le.ScanFilter;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;

import com.example.admin.mybledemo.R;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.Options;

public class FilterView extends BaseFrameLayout {
    private static final String TAG = "FilterView";

    private Context context;
    private PopupWindow popupWindow;
    private InputMethodManager imms;

    private TextView tvFilters;
    private ImageView ivExpand, ivClear;
    private FilterListener filterListener;


    public FilterView(@NonNull Context context) {
        super(context);
    }

    public FilterView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        this.context = context;
    }

    public FilterView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(FilterListener filterListener){
        this.filterListener = filterListener;
    }

    @Override
    protected int layoutId() {
        return R.layout.layout_filterview;
    }

    @Override
    protected void bindData() {
        tvFilters = mView.findViewById(R.id.tv_filters);
        ivExpand = mView.findViewById(R.id.iv_expand);
        ivClear = mView.findViewById(R.id.iv_clear);

        tvFilters.setText("-50dBm");
    }

    @Override
    protected void bindListener() {
        super.bindListener();
        ivExpand.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                show();
            }
        });

        ivClear.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                filterListener.onCancel();
            }
        });
    }

    /**
     * 设置添加屏幕的背景透明度(值越大,透明度越高)
     *
     * @param bgAlpha
     */
    public void backgroundAlpha(Activity context, float bgAlpha) {
        WindowManager.LayoutParams lp = context.getWindow().getAttributes();
        lp.alpha = bgAlpha;
        context.getWindow().addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
        context.getWindow().setAttributes(lp);
    }

    public void show(){
        if (popupWindow != null && imms != null) {
            imms.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
        }
        if (popupWindow == null){
            imms = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            View layout = LayoutInflater.from(context).inflate(R.layout.filter_window, null);
            popupWindow = new PopupWindow(layout,ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, true);
//            popupWindow.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
            imms.toggleSoftInput(0, InputMethodManager.SHOW_FORCED);
            popupWindow.setBackgroundDrawable(new ColorDrawable(0xb0000000));
            popupWindow.setOutsideTouchable(true);

            EditText etNameAddress = layout.findViewById(R.id.et_name_address);
            ImageView ivClear = layout.findViewById(R.id.iv_clear);
            SeekBar sbRssi = layout.findViewById(R.id.sb_rssi);
            TextView tvRssi = layout.findViewById(R.id.tv_rssi);

            etNameAddress.addTextChangedListener(new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {

                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {

                }

                @Override
                public void afterTextChanged(Editable s) {
                    Options options = Ble.options();
                    Log.e(TAG, "afterTextChanged: "+s.toString());
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        String add_name = s.toString();
                        if (!TextUtils.isEmpty(add_name)){
                            boolean isAddress = BluetoothAdapter.checkBluetoothAddress(add_name);
                            ScanFilter.Builder builder = new ScanFilter.Builder();
                            if (isAddress){
                                builder.setDeviceAddress(add_name);
                            }else {
                                builder.setDeviceName(add_name);
                            }
                            options.setScanFilter(builder.build());
                        }else {
                            //清除过滤
                            options.setScanFilter(null);
                        }
                        filterListener.onAddressNameChanged(add_name);
                    }
                }
            });
            ivClear.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    etNameAddress.setText("");
                }
            });
            sbRssi.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {
                    filterListener.onRssiChanged(seekBar.getProgress());
                }
            });

        }
        if (!popupWindow.isShowing()) {
//            popupWindow.showAtLocation(mView, Gravity.BOTTOM, 0, 0);
            popupWindow.showAsDropDown(mView);
        } else {
            popupWindow.dismiss();
        }
    }

    public interface FilterListener {
        void onAddressNameChanged(String addressOrName);
        void onRssiChanged(int rssi);
        void onCancel();
    }


}
