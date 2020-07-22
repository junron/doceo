package com.example.attendance.models.snapmit

import com.example.attendance.models.Students
import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val assignmentId: String,
    val id: String,
    val deleted: Boolean = false,
    val images: List<String>,
    val owner: String,
    val time: Long,
    val comment: String = ""
) {
    val name: String
        get() = Students.getStudentById(owner)!!.name
}
