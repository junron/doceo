package com.example.attendance.util.android

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object Sharing {
    fun shareCSV(context: Context, action: String, file: File): Intent {
        val intent = Intent(action)
        intent.type = "text/csv"
        intent.putExtra(
            Intent.EXTRA_STREAM, FileProvider.getUriForFile(
                context,
                context.applicationContext.packageName + ".provider",
                file
            )
        )
        return intent
    }
}
