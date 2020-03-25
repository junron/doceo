package com.example.attendance.util.android.notifications

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.attendance.util.auth.UserLoader.getMsToken
import com.example.attendance.util.auth.UserLoader.userExists
import com.example.attendance.util.postJson
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object NotificationServer {
    private lateinit var queue: RequestQueue

    fun init(context: Context) {
        queue = Volley.newRequestQueue(context)
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
        val response = queue.postJson(
            "https://pyrostore.nushhwboard.ml/notifications/addToken",
            request,
            AddTokenRequest.serializer()
        )
        println(response)
    }
}
