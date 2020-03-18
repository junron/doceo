package com.example.attendance.util.android

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import java.io.File

object Sharing {
    fun shareImage(context: Context, drawable: Drawable) {
        val bitmap = drawable.toBitmap()
        val file: File
        try {
            file = File(context.filesDir.toString() + "/image.jpg")
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, file.outputStream())
        } catch (e: Exception) {
            println(e)
            return
        }
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "image/*"
        intent.putExtra(
            Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        )
    }
}
