package com.example.attendance.models.snapmit

import com.example.attendance.models.Students
import com.example.attendance.util.TimestampSerializer
import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable

@Serializable
data class Submission(
    val assignmentId: String = "",
    val id: String = "",
    val deleted: Boolean = false,
    val images: List<String>,
    val owner: String,
    @Serializable(with = TimestampSerializer::class)
    val submissionTime: Timestamp = Timestamp.now(),
    val comment: String = ""
) {
    val name: String
        get() = Students.getStudentById(owner)!!.name
}
