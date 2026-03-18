package com.campuslink.core

import android.util.Log

object CampusLog {
    private const val ENABLED = true  // set false before demo APK
    fun d(tag: String, msg: String) { if (ENABLED) Log.d("CL_$tag", msg) }
    fun w(tag: String, msg: String) { if (ENABLED) Log.w("CL_$tag", msg) }
    fun e(tag: String, msg: String) { if (ENABLED) Log.e("CL_$tag", msg) }
}
