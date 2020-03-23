package com.example.attendance.util.android.notifications

import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class MessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)
        GlobalScope.launch {
            ServerConnection.addToken(token)
        }
    }
}
