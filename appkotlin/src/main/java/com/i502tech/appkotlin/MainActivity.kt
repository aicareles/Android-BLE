package com.i502tech.appkotlin

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothGattCharacteristic
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.widget.LinearLayoutManager
import cn.com.heaton.blelibrary.ble.Ble
import cn.com.heaton.blelibrary.ble.callback.*
import cn.com.heaton.blelibrary.ble.model.BleDevice
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

class MainActivity : AppCompatActivity() {
    private val REQUESTCODE: Int = 0x01
    private lateinit var mBle: Ble<BleDevice>
    private var listDatas = mutableListOf<BleDevice>()
    private val adapter = DeviceAdapter(listDatas)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        requestBLEPermission()

        initView()
        initLinsenter()
    }

    private fun initLinsenter() {
        readRssi.setOnClickListener{
            if (mBle.connetedDevices.size > 0)
            mBle.readRssi(mBle.connetedDevices[0], object : BleReadRssiCallback<BleDevice>() {
                override fun onReadRssiSuccess(rssi: Int) {
                    super.onReadRssiSuccess(rssi)
                    toast("读取远程RSSI成功：$rssi")
                }
            })
        }
        sendData.setOnClickListener {
            val list = mBle.connetedDevices
            synchronized(mBle.locker) {
                for (device in list) {
                    val commandBean = CommandBean()
                    AppProtocol.sendCarMoveCommand(device, commandBean.setCarCommand(80, 1))
                }
            }
        }
        updateOta.setOnClickListener {

        }
        requestMtu.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                //此处第二个参数  不是特定的   比如你也可以设置500   但是如果设备不支持500个字节则会返回最大支持数
                mBle.setMTU(mBle.connetedDevices[0].bleAddress, 96, object : BleMtuCallback<BleDevice>() {
                    override fun onMtuChanged(device: BleDevice, mtu: Int, status: Int) {
                        super.onMtuChanged(device, mtu, status)
                        toast("最大支持MTU：$mtu")
                    }
                })
            } else {
                toast("设备不支持MTU")
            }
        }
        sendEntityData.setOnClickListener {

        }
        cancelEntity.setOnClickListener {
            mBle.cancelWriteEntity()
        }
        scan.setOnClickListener {
            listDatas.clear()
            mBle.startScan(bleScanCallback())
        }
    }

    private fun initView() {
        recyclerview.layoutManager = LinearLayoutManager(this)
        recyclerview.adapter = adapter
        adapter.setOnItemClickListener {
            val device = adapter.items[it]
            device.apply {
                if (isConnected){
                    mBle.disconnect(this)
                }else if (!isConnectting){
                    mBle.connect(this, connectCallback())
                }
            }
        }
    }

    private fun requestBLEPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.BLUETOOTH_ADMIN, Manifest.permission.ACCESS_COARSE_LOCATION), REQUESTCODE)
    }


    //初始化蓝牙
    private fun initBLE() {
        mBle = Ble.options().apply {
            logBleExceptions = true
            throwBleException = true
            autoConnect = true
            connectFailedRetryCount = 3
            connectTimeout = 10000L
            scanPeriod = 12000L
            uuid_service = UUID.fromString("0000fee9-0000-1000-8000-00805f9b34fb")
            uuid_write_cha = UUID.fromString("d44bc439-abfd-45a2-b575-925416129600")
        }.create(applicationContext)
        //3、检查蓝牙是否支持及打开
        checkBluetoothStatus()
    }

    //检查蓝牙是否支持及打开
    private fun checkBluetoothStatus() {
        // 检查设备是否支持BLE4.0
        if (!mBle.isSupportBle(this)) {
            toast("该设备不支持BLE蓝牙")
            finish()
        }
        if (!mBle.isBleEnable) {
            //4、若未打开，则请求打开蓝牙
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableBtIntent, Ble.REQUEST_ENABLE_BT)
        } else {
            //5、若已打开，则进行扫描
            mBle.startScan(bleScanCallback())
        }
    }

    private fun bleScanCallback(): BleScanCallback<BleDevice> {
        return object : BleScanCallback<BleDevice>() {
            override fun onLeScan(device: BleDevice?, rssi: Int, scanRecord: ByteArray?) {
                for (d in listDatas) {
                    if (d.bleAddress == device?.bleAddress) {
                        return
                    }
                }
                device?.let {
                    listDatas.add(it)
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    private fun connectCallback(): BleConnectCallback<BleDevice> {
        return object : BleConnectCallback<BleDevice>(){
            override fun onConnectionChanged(device: BleDevice?) {
                if (device?.isConnected == true)
                    mBle.startNotify(device, bleNotifyCallback())
                adapter.notifyDataSetChanged()
            }

            override fun onConnectException(device: BleDevice?, errorCode: Int) {
                super.onConnectException(device, errorCode)
                toast("连接异常，异常状态码:$errorCode")
            }

            override fun onConnectTimeOut(device: BleDevice?) {
                super.onConnectTimeOut(device)
                toast("连接异常，异常状态码:${device?.bleName}")
            }

        }
    }

    private fun bleNotifyCallback(): BleNotiftCallback<BleDevice> {
        return object : BleNotiftCallback<BleDevice>(){
            override fun onChanged(device: BleDevice?, characteristic: BluetoothGattCharacteristic?) {

            }

        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUESTCODE) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    // 判断用户是否 点击了不再提醒。(检测该权限是否还可以申请)
                    val b = shouldShowRequestPermissionRationale(permissions[0])
                    if (!b) {
                        // 用户还是想用我的 APP 的
                        // 提示用户去应用设置界面手动开启权限
                    } else
                        finish()
                } else {
                    //权限申请成功
                    initBLE()
                }
            }
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUESTCODE) {
            if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val i = ContextCompat.checkSelfPermission (this, Manifest.permission.ACCESS_COARSE_LOCATION)
                if (i != PackageManager.PERMISSION_GRANTED) {
                    // 提示用户应该去应用设置界面手动开启权限
                } else {
                    //权限申请成功
                    initBLE()
                }
            }
        }
    }

}