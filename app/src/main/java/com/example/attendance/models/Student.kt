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
data class StatefulStudent(val student: Student, var tag: Tag)

object Students {
    var students: List<Student> = emptyList()
        private set

    @UnstableDefault
    fun loadStudents(context: Context) {
        val fileData = String(context.assets.open("y4.json").readBytes())
        students = Json.parse(Student.serializer().list, fileData)
    }

    fun getStudentById(id: String) = students.find { it.id == id }
    fun getStudentByName(name: String) = students.find { it.name == name }
    fun filterStudents(
        query: List<String>,
        _students: List<Student> = Students.students
    ): List<Student> {
        var students = _students
        query.forEach {
            if (":" !in it) return@forEach
            val (key, value) = it.split(":")
            val filterParam = FilterParam.filterParams.firstOrNull { param ->
                param.key == "$key: "
            } ?: return@forEach
            if (value !in filterParam.possibleValues) return@forEach
            when (filterParam.key) {
                "from: " -> students = students.filter { student ->
                    student.mentorGroup.substringAfter("M20") == value
                }
                "takes: " -> students = students.filter { student ->
                    value in student.combination
                }
            }
        }
        return students
    }
}
