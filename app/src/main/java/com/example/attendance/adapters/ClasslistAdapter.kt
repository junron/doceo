package com.example.attendance.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.attendance.models.StatefulStudent
import com.example.attendance.models.Student
import com.example.attendance.models.Students

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
        students = Students.filterStudents(query, originalStudents.map { it.student }).map {
            StatefulStudent(it, 0)
        }
        notifyDataSetChanged()
    }

    override fun getItem(position: Int): StatefulStudent {
        return students[position]
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = students.size
}
