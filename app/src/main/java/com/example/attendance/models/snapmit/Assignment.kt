package com.example.attendance.models.snapmit

import com.google.firebase.Timestamp

data class Assignment(
    // UUID
    val id: String = "",
    val deleted: Boolean = false,
    val name: String = "Unknown assignment",
    val submissions: List<String> = emptyList(),
    val students: List<String> = emptyList(),
    val owner: String = "",
    val dueDate: Timestamp = Timestamp.now(),
    val description: String = "No description provided"
)
