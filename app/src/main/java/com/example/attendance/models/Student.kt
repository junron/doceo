package com.example.attendance.models

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.builtins.list
import kotlinx.serialization.json.Json

@Serializable
data class Student(
    val id: String,
    val name: String,
    val shortName: String,
    val mentorGroup: String,
    val combination: List<String>
)

@Serializable
data class StatefulStudent(val student: Student, var state: Int)

object Students {
    var students: List<Student> = emptyList()
        private set

    @UnstableDefault
    fun loadStudents(context: Context) {
        val fileData = String(context.assets.open("y4.json").readBytes())
        students = Json.parse(Student.serializer().list, fileData)
    }

    fun getStudentById(id: String) = students.find { it.id == id.substringBefore("@") }
}
