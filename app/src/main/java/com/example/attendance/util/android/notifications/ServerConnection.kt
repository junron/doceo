package com.example.attendance.util.android.notifications

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley
import com.example.attendance.util.auth.UserLoader.getMsToken
import com.example.attendance.util.postJson

object ServerConnection {
    private lateinit var queue: RequestQueue

    fun init(context: Context) {
        queue = Volley.newRequestQueue(context)
    }

    suspend fun addToken(token: String) {
        val request = AddTokenRequest(token, getMsToken())
        val response = queue.postJson(
            "https://pyrostore.nushhwboard.ml/notifications/addToken",
            request,
            AddTokenRequest.serializer()
        )
        println(response)
    }
}
