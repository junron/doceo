package com.example.attendance.util.android

import com.example.attendance.util.uuid
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

object CloudStorage {
    private val storage = Firebase.storage
    private val root = storage.reference
    suspend fun addObject(file: File) = suspendCancellableCoroutine<String> { cont ->
        val fileName = "${uuid()}.${file.extension}"
        val fileRef = root.child(fileName)
        val task = fileRef.putStream(file.inputStream())
        cont.invokeOnCancellation {
            task.cancel()
        }
        task.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let {
                    throw it
                }
            }
            fileRef.downloadUrl
        }.addOnSuccessListener {
            cont.resume(it.toString())
        }.addOnFailureListener {
            cont.resumeWithException(it)
        }
    }
}
