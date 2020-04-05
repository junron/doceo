package com.example.attendance.models

import android.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
data class Tag(val id: String, val name: String, val color: Int)

object Tags {
    const val present = "182c857c-d0ec-440e-973e-5a924407ea8c"
    const val absent = "0812b43d-1690-450b-8fb3-26b16c0b9fb3"
    val defaultTags = listOf(
        Tag(absent, "Absent", Color.RED),
        Tag(present, "Present", Color.GREEN)
    )
}
