package com.example.attendance.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.attendance.models.FilterParam
import com.example.attendance.models.StatefulStudent
import com.example.attendance.models.Student

class ClasslistAdapter(private val originalStudents: List<StatefulStudent>) : BaseAdapter() {
    companion object {
        fun createAdapter(students: List<Student>) =
            ClasslistAdapter(students.map {
                StatefulStudent(it, 0)
            })
    }

    private var students = originalStudents
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val statefulStudent = getItem(position)
        val (student, state) = statefulStudent
        return with(TextView(parent.context)) {
            text = student.shortName
            setTextColor(
                if (state == 1) Color.GREEN else Color.RED
            )
            setPadding(36, 24, 10, 36)
            setOnClickListener {
                statefulStudent.state++
                statefulStudent.state %= 2
                notifyDataSetChanged()
            }
            gravity = Gravity.CENTER
            textSize = 18f
            this
        }
    }

    fun filterStudents(query: List<String>) {
        students = originalStudents
        query.forEach {
            if (":" !in it) return@forEach
            val (key, value) = it.split(":")
            val filterParam = FilterParam.filterParams.firstOrNull { param ->
                param.key == "$key: "
            } ?: return@forEach
            if (value !in filterParam.possibleValues) return@forEach
            when (filterParam.key) {
                "from: " -> students = students.filter { student ->
                    student.student.mentorGroup.substringAfter("M20") == value
                }
                "takes: " -> students = students.filter { student ->
                    value in student.student.combination
                }
            }
            notifyDataSetChanged()
        }
    }

    override fun getItem(position: Int): StatefulStudent {
        return students[position]
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = students.size
}
