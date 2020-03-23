package com.example.attendance.util

import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

suspend fun <T> RequestQueue.postJson(url: String, data: T, serializer: KSerializer<T>) =
    suspendCoroutine<String> { cont ->
        this.add(generateObject(url, data, serializer, cont))
    }

private fun <T> generateObject(
    url: String,
    data: T,
    serializer: KSerializer<T>,
    cont: Continuation<String>
) =
    object : StringRequest(Method.POST, url, { response ->
        cont.resume(response)
    }, { error ->
        cont.resumeWithException(error)
    }) {
        override fun getBody() = (Json.stringify(serializer, data)).toByteArray()

        override fun getBodyContentType() = "application/json"
    }
