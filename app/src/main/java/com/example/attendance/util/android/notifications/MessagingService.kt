package com.example.attendance.util.android.notifications

import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.notifications.Notifications.notify
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch


class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        GlobalScope.launch {
            NotificationServer.addToken(token)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val type = remoteMessage.data["type"] ?: return
        when (type) {
            "GENERIC_NOTIFICATION" -> {
                val notification = Notifications.getBuilder(applicationContext).apply {
                    setContentTitle(remoteMessage.data["title"])
                    setContentText(remoteMessage.data["content"])
                    setSmallIcon(R.drawable.ic_attendance)
                    setVibrate(longArrayOf(0, 250))
                    priority = NotificationCompat.PRIORITY_HIGH
                }.build()
                notify(applicationContext, notification)
            }
            "ITEM_SHARED" -> {
                val notification = Notifications.getBuilder(applicationContext).apply {
                    val owner by remoteMessage.data
                    val attendanceName by remoteMessage.data
                    val attendanceId by remoteMessage.data
                    println(attendanceId)

                    val intent = PendingIntent.getActivity(
                        applicationContext, 0,
                        Intent(
                            applicationContext,
                            MainActivity::class.java
                        ).apply {
                            putExtra("updateApp", false)
                            putExtra("attendance_id", attendanceId)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }, PendingIntent.FLAG_ONE_SHOT
                    )

                    setContentIntent(intent)
                    setContentTitle(owner)
                    setContentText("shared a classlist with you: $attendanceName")
                    setSmallIcon(R.drawable.ic_baseline_group_24)
                    setVibrate(longArrayOf(0, 250))
                    setAutoCancel(true)
                    priority = NotificationCompat.PRIORITY_HIGH
                }.build()
                notify(applicationContext, notification)
            }
            "APP_UPDATE" -> {
                println("App update")
                val notification = Notifications.getBuilder(applicationContext).apply {
                    val intent = PendingIntent.getActivity(
                        applicationContext, 0,
                        Intent(
                            applicationContext,
                            MainActivity::class.java
                        ).apply {
                            putExtra("updateApp", true)
                            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                        }, PendingIntent.FLAG_ONE_SHOT
                    )

                    setContentIntent(intent)
                    setContentTitle("New version available!")
                    setContentText("Tap to install")
                    setSmallIcon(R.drawable.ic_update)
                    setVibrate(longArrayOf(0, 250))
                    setAutoCancel(true)
                    priority = NotificationCompat.PRIORITY_HIGH
                }.build()
                notify(applicationContext, notification)
            }
        }
    }
}
