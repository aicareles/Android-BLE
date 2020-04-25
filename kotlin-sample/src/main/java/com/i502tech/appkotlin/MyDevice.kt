package com.i502tech.appkotlin

import cn.com.heaton.blelibrary.ble.model.BleDevice

/**
 * author: jerry
 * date: 20-4-25
 * email: superliu0911@gmail.com
 * des:  自定义BleDevice对象
 */
class MyDevice(address: String, name: String): BleDevice(address, name) {
    var onLight: Boolean = false //是否开灯状态
    var type = 0 //产品类型
    var parent = 0 //产品父类型
}