package com.example.admin.mybledemo.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.widget.TextView;

import com.example.admin.mybledemo.BleRssiDevice;
import com.example.admin.mybledemo.R;

import java.util.List;

import cn.com.heaton.blelibrary.ble.model.BleDevice;

public class ScanAdapter extends RecyclerAdapter<BleRssiDevice> {

    public ScanAdapter(Context context, int layoutId, List<BleRssiDevice> datas) {
        super(context, layoutId, datas);
    }

    @Override
    public void convert(RecyclerViewHolder hepler, BleRssiDevice rssiDevice) {
        TextView rssi = hepler.getView(R.id.tv_rssi);
        TextView name = hepler.getView(R.id.tv_name);
        TextView address = hepler.getView(R.id.tv_address);
        BleDevice device = rssiDevice.getDevice();
        rssi.setText(String.format("%ddBm", rssiDevice.getRssi()));
        if (TextUtils.isEmpty(device.getBleName())){
            name.setText("未知设备");
        }else {
            name.setText(device.getBleName());
        }
        address.setText(device.getBleAddress());
    }

}
