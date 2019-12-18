package com.example.admin.mybledemo;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;
import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.BleDevice;

/**
 *
 * Created by LiuLei on 2016/11/26.
 */


public class DeviceAdapter extends BaseAdapter {
    private List<BleDevice> mLeDevices;
    private Context mContext;

    public DeviceAdapter(Context context, List<BleDevice> devices) {
        mLeDevices = devices;
        mContext = context;
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public BleDevice getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        final ViewHolder viewHolder;
        if (view == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceState = (TextView) view.findViewById(R.id.state);
            viewHolder.cancelReConnect = (Button) view.findViewById(R.id.cancelReConnect);
            view.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) view.getTag();
        }

        final BleDevice device = mLeDevices.get(i);
        final String deviceName = device.getBleName();
        if(device.isConnectting()){
            viewHolder.deviceState.setText("正在连接中...");
        }
        else if(device.isConnected()){
            viewHolder.deviceState.setText("已连接");
        }else {
            viewHolder.deviceState.setText("未连接");
        }
        if (TextUtils.isEmpty(deviceName)){
            viewHolder.deviceName.setText("未知设备");
        }else {
            viewHolder.deviceName.setText(deviceName);
        }

        if (device.isAutoConnect()){
            viewHolder.cancelReConnect.setText("取消重连");
        }else {
            viewHolder.cancelReConnect.setText("重连");
        }
        viewHolder.deviceAddress.setText(device.getBleAddress());
        viewHolder.cancelReConnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (device.isAutoConnect()){
                    Ble.getInstance().resetReConnect(device, false);
                    Utils.showToast("已取消重连");
                    viewHolder.cancelReConnect.setText("重连");
                    if (!device.isConnected()){
                        viewHolder.deviceState.setText("未连接");
                    }
                }else {
                    Ble.getInstance().resetReConnect(device, true);
                    Utils.showToast("开启重连");
                    viewHolder.cancelReConnect.setText("取消重连");
                }
            }
        });

        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
        TextView deviceState;
        Button cancelReConnect;
    }

}
