package com.example.attendance.util.android

import com.example.attendance.util.uuid
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import java.io.File

object CloudStorage {
    private val storage = Firebase.storage
    private val root = storage.reference
    fun addObject(file: File, callback: (String) -> Unit) {
        val fileName = "${uuid()}.${file.extension}"
        val fileRef = root.child(fileName)
        fileRef.putStream(file.inputStream())
            .continueWithTask { task ->
                if (!task.isSuccessful) {
                    task.exception?.let {
                        throw it
                    }
                }
                fileRef.downloadUrl
            }.addOnSuccessListener {
                callback(it.toString())
            }.addOnFailureListener {
                println("Failed: $it")
            }
    }
}
