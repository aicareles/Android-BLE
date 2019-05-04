package com.i502tech.appkotlin

import android.content.Context
import android.widget.Toast

/**
 * description $desc$
 * created by jerry on 2019/5/4.
 */
fun Context.toast(message: String?, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, message, duration).show()
}