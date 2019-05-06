package com.i502tech.appkotlin

import android.content.Context
import android.widget.Toast
import cn.com.heaton.blelibrary.ble.utils.ByteUtils
import java.io.InputStream

/**
 * description $desc$
 * created by jerry on 2019/5/4.
 */
fun Context.toast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}

fun ByteUtils.toByteArray(input: InputStream): ByteArray {
    var offset = 0
    var remaining = input.available()
    val result = ByteArray(remaining)
    while (remaining > 0){
        val read = input.read(result, offset, remaining)
        if (read < 0)break
        remaining -= read
        offset+=read
    }
    if (remaining == 0){
        return result
    } else return result.copyOf(offset)
}