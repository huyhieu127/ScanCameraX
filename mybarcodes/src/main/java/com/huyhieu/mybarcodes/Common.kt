package com.huyhieu.mybarcodes

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.DialogInterface
import android.util.DisplayMetrics
import androidx.appcompat.app.AlertDialog


val CAMERA_REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)

fun Activity.showDialogAlert(s: String, listener: DialogInterface.OnClickListener?) {
    AlertDialog.Builder(this)
        .setTitle("Information")
        .setMessage(s)
        .setCancelable(false)
        .setPositiveButton(android.R.string.yes, listener)
        //.setNegativeButton(android.R.string.no, null)
        //.setIcon(android.R.drawable.ic_dialog_info)
        .show()
}

fun Context.checkAllPermission(lstPermission: Array<String>) = lstPermission.all {
    androidx.core.content.ContextCompat.checkSelfPermission(
        this, it
    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
}

fun Context.convertPixelsToDp(px: Float): Float {
    return px / (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}
fun Context.convertDpToPixel(dp: Float): Float {
    return dp * (resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)
}