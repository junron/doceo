package com.example.attendance.util.android

import android.app.Notification
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.attendance.util.android.Notifications.notify

class NotificationPublisher : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val notification = intent.getParcelableExtra<Notification>("notification")
        val notificationId = intent.getIntExtra("notification_id", 0)
        notify(context, notification, notificationId)
    }

}
