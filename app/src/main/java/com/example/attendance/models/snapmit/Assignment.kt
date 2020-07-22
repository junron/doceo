package com.example.attendance.models.snapmit

import kotlinx.serialization.Serializable

@Serializable
data class Assignment(
    // UUID
    val id: String,
    val deleted: Boolean = false,
    val name: String = "Unknown assignment",
    val submissions: List<Submission>
)
