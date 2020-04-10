package com.example.attendance.util.android.notifications

import com.example.attendance.util.Volley.queue
import com.example.attendance.util.auth.UserLoader.getMsToken
import com.example.attendance.util.auth.UserLoader.userExists
import com.example.attendance.util.postJson
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NotificationServer {

    fun init() {
        if (!userExists()) return
        FirebaseInstanceId.getInstance().instanceId
            .addOnCompleteListener { task ->
                if (!task.isSuccessful) {
                    println("Failed")
                    return@addOnCompleteListener
                }

                // Get new Instance ID token
                val token = task.result?.token ?: return@addOnCompleteListener
                GlobalScope.launch {
                    addToken(token)
                }
            }

    }

    suspend fun addToken(token: String) {
        if (!userExists()) return
        val request = AddTokenRequest(token, getMsToken())
        queue.postJson(
            "https://pyrostore.nushhwboard.ml/notifications/addToken",
            request,
            AddTokenRequest.serializer()
        )
    }

    suspend fun sendNotification(sendNotificationRequest: SendNotificationRequest) {
        if (!userExists()) return
        queue.postJson(
            "https://pyrostore.nushhwboard.ml/notifications/sendNotification",
            sendNotificationRequest.copy(auth = getMsToken()),
            SendNotificationRequest.serializer()
        )
    }
}
