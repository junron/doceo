package com.example.attendance.util.android

import android.app.Activity
import androidx.core.app.ActivityCompat

object Permissions {
    fun requestPermissions(activity: Activity, permission: String) {
        // No explanation needed, we can request the permission.
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(permission),
            0
        )
    }
}
