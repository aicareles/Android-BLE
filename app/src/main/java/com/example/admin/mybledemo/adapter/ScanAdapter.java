package com.example.admin.mybledemo.adapter;

import android.content.Context;
import android.content.Intent;
import android.os.ParcelUuid;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.example.admin.mybledemo.BleRssiDevice;
import com.example.admin.mybledemo.ui.DeviceInfoActivity;
import com.example.admin.mybledemo.R;

import java.util.List;

import cn.com.heaton.blelibrary.ble.Ble;
import cn.com.heaton.blelibrary.ble.model.BleDevice;
import cn.com.heaton.blelibrary.ble.model.ScanRecord;
import cn.com.heaton.blelibrary.ble.utils.ByteUtils;

public class ScanAdapter extends RecyclerAdapter<BleRssiDevice> {

    private int opened = -1;

    public ScanAdapter(Context context, List<BleRssiDevice> datas) {
        super(context, R.layout.item_scan, datas);
    }

    @Override
    public void convert(RecyclerViewHolder hepler, BleRssiDevice rssiDevice) {
        TextView rssi = hepler.getView(R.id.tv_rssi);
        TextView name = hepler.getView(R.id.tv_name);
        TextView address = hepler.getView(R.id.tv_address);

        LinearLayout ll_detail = hepler.getView(R.id.ll_detail);
        TextView tv_device_type = hepler.getView(R.id.tv_device_type);
        TextView tv_advertise_type = hepler.getView(R.id.tv_advertise_type);
        TextView tv_flags = hepler.getView(R.id.tv_flags);
        TextView tv_uuid = hepler.getView(R.id.tv_uuid);
        TextView tv_local_name = hepler.getView(R.id.tv_local_name);
        TextView tv_tx_power_level = hepler.getView(R.id.tv_tx_power_level);
        TextView tv_manufacturer_data = hepler.getView(R.id.tv_manufacturer_data);


        BleDevice device = rssiDevice.getDevice();
        rssi.setText(String.format("%ddBm", rssiDevice.getRssi()));
        if (TextUtils.isEmpty(device.getBleName())){
            name.setText("未知设备");
        }else {
            name.setText(device.getBleName());
        }
        address.setText(device.getBleAddress());

        if (hepler.getAdapterPosition() == opened){
            ll_detail.setVisibility(View.VISIBLE);
        } else {
            ll_detail.setVisibility(View.GONE);
        }

        hepler.getView(R.id.tv_connect).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Ble<BleDevice> ble = Ble.getInstance();
                if (ble.isScanning()) {
                    ble.stopScan();
                }
                mContext.startActivity(new Intent(
                        mContext,
                        DeviceInfoActivity.class)
                        .putExtra(DeviceInfoActivity.EXTRA_TAG, device));
            }
        });

        hepler.getConvertView().setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(opened == hepler.getAdapterPosition()) {
                    //当点击的item已经被展开了, 就关闭.
                    opened = -1;
                    notifyItemChanged(hepler.getAdapterPosition());
                }else {
                    int oldOpened = opened;
                    opened = hepler.getAdapterPosition();
                    notifyItemChanged(oldOpened);
                    notifyItemChanged(opened);
                }
            }
        });

        ScanRecord scanRecord = rssiDevice.getScanRecord();
        Log.e("scanRecord", "convert: "+scanRecord.toString());
        if (scanRecord != null){
            scanRecord.getAdvertiseFlags();
            tv_device_type.setText("Device type: LE only");
            tv_advertise_type.setText("Advertising type: Legacy");
            tv_flags.setText("Flags:");
            List<ParcelUuid> serviceUuids = scanRecord.getServiceUuids();
            if (serviceUuids != null && !serviceUuids.isEmpty()){
                tv_uuid.setText(String.format("Service Uuids: %s", TextUtils.join(", ", serviceUuids)));
            }
            String localName = scanRecord.getDeviceName();
            if (!TextUtils.isEmpty(localName)){
                tv_local_name.setText("Local Name: "+localName);
            }
            if (scanRecord.getTxPowerLevel()>-100 && scanRecord.getTxPowerLevel()<=0){
                tv_tx_power_level.setText(String.format("Tx Power Level: %d dBm", scanRecord.getTxPowerLevel()));
            }

            SparseArray<byte[]> array = scanRecord.getManufacturerSpecificData();
            if (array.size() > 0){
                StringBuilder builder = new StringBuilder();
                builder.append("Company: Reserved ID<");
                for (int i = 0; i < array.size(); i++) {
                    builder.append("0x").append(Integer.toHexString(array.keyAt(i))).append(">");
                    builder.append(ByteUtils.bytes2HexStr(array.valueAt(i)));
                }
                builder.append("");
                tv_manufacturer_data.setText(builder.toString());
            }

        }
    }

}
