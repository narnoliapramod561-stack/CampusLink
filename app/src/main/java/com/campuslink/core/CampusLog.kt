package com.campuslink.core

object CampusLog {
    private const val ENABLED = true
    fun d(tag: String, msg: String) { if (ENABLED) android.util.Log.d("CL_$tag", msg) }
    fun w(tag: String, msg: String) { if (ENABLED) android.util.Log.w("CL_$tag", msg) }
    fun e(tag: String, msg: String) { if (ENABLED) android.util.Log.e("CL_$tag", msg) }
}
