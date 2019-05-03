package com.example.admin.mybledemo.adapter;

import android.app.Activity;
import android.bluetooth.BluetoothDevice;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;


import com.example.admin.mybledemo.R;

import java.util.ArrayList;
import java.util.List;

import cn.com.heaton.blelibrary.ble.BleDevice;

/**
 *
 * Created by LiuLei on 2016/11/26.
 */


public class LeDeviceListAdapter extends BaseAdapter {
    private ArrayList<BleDevice> mLeDevices;
    private LayoutInflater mInflator;

    public LeDeviceListAdapter(Activity context) {
        super();
        mLeDevices = new ArrayList<BleDevice>();
        mInflator = context.getLayoutInflater();
    }

    public void addDevice(BleDevice device) {
        for (BleDevice d : mLeDevices){
            if(d.getBleAddress().equals(device.getBleAddress())){
                return;
            }
        }
        mLeDevices.add(device);
//        if (!mLeDevices.contains(device)) {
//            mLeDevices.add(device);
//        }
    }


    public void addDevices(List<BleDevice> devices){
        for(BleDevice device : devices){
            if(!mLeDevices.contains(device)){
                mLeDevices.add(device);
            }
        }
    }

    public BleDevice getDevice(int position) {
        return mLeDevices.get(position);
    }

    public void clear() {
        mLeDevices.clear();
    }

    @Override
    public int getCount() {
        return mLeDevices.size();
    }

    @Override
    public Object getItem(int i) {
        return mLeDevices.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        ViewHolder viewHolder;
        // General ListView optimization code.
        if (view == null) {
            view = mInflator.inflate(R.layout.listitem_device, null);
            viewHolder = new ViewHolder();
            viewHolder.deviceAddress = (TextView) view.findViewById(R.id.device_address);
            viewHolder.deviceName = (TextView) view.findViewById(R.id.device_name);
            viewHolder.deviceState = (TextView) view.findViewById(R.id.state);
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

        viewHolder.deviceAddress.setText(device.getBleAddress());

        return view;
    }

    class ViewHolder {
        TextView deviceName;
        TextView deviceAddress;
        TextView deviceRSSI;
        TextView deviceState;
    }

}
