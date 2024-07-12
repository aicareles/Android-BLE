package com.i502tech.appkotlin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import cn.com.heaton.blelibrary.ble.model.BleDevice
import kotlinx.android.synthetic.main.item_device.view.*

/**
 * description $desc$
 * created by jerry on 2019/5/4.
 */
class DeviceAdapter(var items: List<BleDevice>) : RecyclerView.Adapter<DeviceAdapter.ViewHolder>() {

    private var mListener: ((Int) -> Unit?)? = null

    fun setOnItemClickListener(mListener: (Int) -> Unit) {
        this.mListener = mListener
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_device, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val device = items[position]
        device.run {
            holder.itemView.device_name.text = bleName ?: "未知设备"
            holder.itemView.device_address.text = bleAddress
            when {
                device.isConnected -> holder.itemView.state.text = "已连接"
                device.isConnecting -> holder.itemView.state.text = "正在连接中..."
                else -> holder.itemView.state.text = "未连接"
            }
        }
        holder.itemView.setOnClickListener {
            mListener?.invoke(position)
        }
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
}