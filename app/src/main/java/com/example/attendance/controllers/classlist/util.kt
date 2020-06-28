package com.example.attendance.controllers.classlist

import com.example.attendance.util.isToday
import com.example.attendance.util.isYesterday
import java.text.SimpleDateFormat
import java.util.*

fun formatDate(date: Date, seconds: Boolean = false): String {
    val sdf = SimpleDateFormat("dd MMM")
    val sdf2 = SimpleDateFormat("hh:mm${if (seconds) ":ss" else ""} a")
    val day: String = when {
        date.isToday() -> "Today"
        date.isYesterday() -> "Yesterday"
        else -> sdf.format(date)
    }
    return "$day at ${sdf2.format(date)}"
}
