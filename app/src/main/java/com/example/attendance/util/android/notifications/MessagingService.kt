package com.example.attendance.util.android.notifications

import androidx.core.app.NotificationCompat
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
                    setSmallIcon(R.drawable.ic_baseline_group_24)
                    setVibrate(longArrayOf(0, 250))
                    priority = NotificationCompat.PRIORITY_HIGH
                }.build()
                notify(applicationContext, notification)
            }
        }
    }
}
