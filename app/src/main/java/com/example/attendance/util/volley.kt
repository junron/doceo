package com.example.attendance.util

import android.content.Context
import com.android.volley.NetworkResponse
import com.android.volley.Request
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.HttpHeaderParser
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json
import java.io.File
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object Volley {
    lateinit var queue: RequestQueue
    fun init(context: Context) {
        queue = Volley.newRequestQueue(context)
    }
}
suspend fun <T> RequestQueue.postJson(
    url: String,
    data: T,
    serializer: KSerializer<T>,
    error: ((Exception) -> Unit) = {
        print(it)
    }
) =
    suspendCoroutine<String> { cont ->
        this.add(generateObject(url, data, serializer, cont, error))
    }

private fun <T> generateObject(
    url: String,
    data: T,
    serializer: KSerializer<T>,
    cont: Continuation<String>,
    errorHandler: ((Exception) -> Unit)?
) =
    object : StringRequest(Method.POST, url, { response ->
        cont.resume(response)
    }, { error ->
        when {
            errorHandler != null -> {
                errorHandler(error)
            }
            error is com.android.volley.NoConnectionError -> {
                println("No connection.")
            }
            else -> cont.resumeWithException(error)
        }
    }) {
        override fun getBody() = (Json.stringify(serializer, data)).toByteArray()

        override fun getBodyContentType() = "application/json"
    }


suspend fun RequestQueue.downloadFile(baseFilesDir: File, url: String) =
    suspendCoroutine<File> { cont ->
        GlobalScope.launch {
            val data = suspendCoroutine<ByteArray> { cont2 ->
                add(getDownloadObject(url, cont2))
            }
            val outputFile = baseFilesDir.resolve(url.split("/").last())
            outputFile.writeBytes(data)
            cont.resume(outputFile)
        }
    }


private fun getDownloadObject(
    url: String,
    cont: Continuation<ByteArray>
) = object :
    Request<ByteArray>(Method.GET, url, { error ->
        if (error is com.android.volley.NoConnectionError) {
            println("No connection.")
        } else cont.resumeWithException(error)
    }) {
    override fun parseNetworkResponse(response: NetworkResponse) =
        Response.success(response.data, HttpHeaderParser.parseCacheHeaders(response))

    override fun deliverResponse(response: ByteArray) {
        cont.resume(response)
    }

}
