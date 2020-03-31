package com.example.attendance.models

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Tag(val name: String, val color: Int)

object Tags {
    val defaultTags = listOf(
        Tag("Absent", Color.RED),
        Tag("Present", Color.GREEN)
    )
}
