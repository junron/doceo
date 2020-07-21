package com.example.attendance.models.snapmit

import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val id: String,
    val deleted: Boolean = false,
    val images: List<String>,
    val owner: String,
    val name: String,
    val comment: String = ""
)
