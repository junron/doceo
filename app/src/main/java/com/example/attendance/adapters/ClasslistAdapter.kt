package com.example.attendance.adapters

import android.graphics.Color
import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.models.StatefulStudent
import com.example.attendance.models.Student
import com.example.attendance.models.Students

class ClasslistAdapter(
    private val originalStudents: List<StatefulStudent>,
    private val editable: Boolean
) : RecyclerView.Adapter<ClasslistAdapter.StudentViewHolder>() {

    companion object {
        fun createAdapter(students: List<Student>, editable: Boolean = false) =
            ClasslistAdapter(students.map {
                StatefulStudent(it, 0)
            }, editable)
    }

    private var students = originalStudents

    class StudentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudentViewHolder(TextView(parent.context))

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val statefulStudent = getItem(position)
        val (student, state) = statefulStudent
        holder.textView.apply {
            text = student.shortName
            setTextColor(
                if (state == 1) Color.GREEN else Color.RED
            )
            setPadding(36, 24, 10, 36)
            if (editable)
                setOnClickListener {
                    statefulStudent.state++
                    statefulStudent.state %= 2
                    notifyDataSetChanged()
                }
            gravity = Gravity.CENTER
            textSize = 18f
        }
    }


    fun filterStudents(query: List<String>) {
        students = Students.filterStudents(query, originalStudents.map { it.student }).map {
            StatefulStudent(it, 0)
        }
        notifyDataSetChanged()
    }

    private fun getItem(position: Int): StatefulStudent {
        return students[position]
    }

    override fun getItemCount() = students.size
}
