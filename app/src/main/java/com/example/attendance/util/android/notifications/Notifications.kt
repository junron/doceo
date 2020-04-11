package com.example.attendance.util.android.notifications


import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.attendance.MainActivity
import com.example.attendance.util.uuid
import java.util.*


object Notifications {
    private val random = Random()
    private lateinit var id: String
    fun createNotificationChannel(context: Context, name: String, descriptionText: String) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        val id = uuid()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(id, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        Notifications.id = id
    }

    fun getBuilder(context: Context): NotificationCompat.Builder {
        if (!::id.isInitialized) createNotificationChannel(
            context,
            "NUSH Attendance",
            "NUS High attendance"
        )
        return NotificationCompat.Builder(
            context,
            id
        )
    }

    fun notify(
        context: Context,
        notification: Notification,
        id: Int = random.nextInt()
    ): Int {
        NotificationManagerCompat.from(context)
            .notify(id, notification)
        return id
    }

    fun scheduleNotification(
        context: Context,
        delay: Long,
        builder: NotificationCompat.Builder,
        notificationId: Int = random.nextInt()
    ) {
        val intent = Intent(context, MainActivity::class.java)
        val activity = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        builder.setContentIntent(activity)
        val notification = builder.build()
        val notificationIntent = Intent(context, NotificationPublisher::class.java)
        notificationIntent.putExtra("notification", notification)
        notificationIntent.putExtra("notification_id", notificationId)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId,
            notificationIntent,
            PendingIntent.FLAG_CANCEL_CURRENT
        )
        val futureInMillis = SystemClock.elapsedRealtime() + delay
        val alarmManager =
            context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager[AlarmManager.ELAPSED_REALTIME_WAKEUP, futureInMillis] = pendingIntent
    }
}
