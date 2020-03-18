package com.example.attendance.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.attendance.models.StatefulStudent
import com.example.attendance.models.Student
import java.util.*

class ClasslistAdapter(private val students: List<StatefulStudent>) : BaseAdapter() {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        println(count)
        val statefulStudent = getItem(position)
        val (student, state) = statefulStudent
        return with(TextView(parent.context)) {
            text = student.shortName
            setTextColor(
                if (state == 1) Color.GREEN else Color.RED
            )
            setPadding(36, 24, 10, 36)
            setOnClickListener {
                val file = parent.context.filesDir.resolve("data")
                file.writeText(
                    file.readText() + "\n${student.name} $state ${Date().time}"
                )
                statefulStudent.state++
                statefulStudent.state %= 2
                notifyDataSetChanged()
            }
            gravity = Gravity.CENTER
            textSize = 18f
            this
        }
    }

    override fun getItem(position: Int): StatefulStudent {
        return students[position]
    }

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount() = students.size
}

fun createAdapter(students: List<Student>) =
    ClasslistAdapter(students.map {
        StatefulStudent(it, 0)
    })
