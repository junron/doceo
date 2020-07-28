package com.example.attendance.fragments.snapmit.submit

import android.util.Log
import com.example.attendance.util.android.CloudStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.io.File

class Uploader {
    companion object {
        fun upload(files: List<File>): List<String> {
            var urls: List<String> = listOf()
            GlobalScope.launch(Dispatchers.IO) {
                urls = files.map {
                    CloudStorage.addObject(it)
                }
            }
            while (urls.isEmpty()) {}
            Log.d("UPLOAD", urls[0])
            return urls
        }
    }
}