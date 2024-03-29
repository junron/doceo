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

    val mentorReps = listOf(
        "h1610044@nushigh.edu.sg",
        "h1710004@nushigh.edu.sg",
        "h1710011@nushigh.edu.sg",
        "h1710021@nushigh.edu.sg",
        "h1710088@nushigh.edu.sg",
        "h1710076@nushigh.edu.sg",
        "h1710092@nushigh.edu.sg",
        "h1710078@nushigh.edu.sg",
        "h1710072@nushigh.edu.sg",
        // "h1710074@nushigh.edu.sg",
        "h1710159@nushigh.edu.sg",
        "h1930009@nushigh.edu.sg"
    )
    fun createAssignmentAuthorized(email: String) =
        (email in mentorReps) or (email.endsWith("@nus.edu.sg"))

    @UnstableDefault
    fun loadStudents(context: Context) {
        val fileData = String(context.assets.open("y4.json").readBytes())
        students = Json.parse(Student.serializer().list, fileData)
    }

    fun getStudentByEmail(id: String) = students.find { it.id == id }
    fun getStudentByName(name: String) = students.find { it.name == name }
    fun filterStudents(
        query: List<String>,
        _students: List<Student> = students
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

