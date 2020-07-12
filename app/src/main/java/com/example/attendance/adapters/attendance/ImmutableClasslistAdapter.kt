package com.example.attendance.adapters.attendance

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.models.Student
import com.example.attendance.models.Students


class ImmutableClasslistAdapter(private val _students: List<Student>) :
    RecyclerView.Adapter<ImmutableClasslistAdapter.StudentViewHolder>() {

    private var students = _students

    class StudentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudentViewHolder(
            TextView(parent.context)
        )

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val student = getItem(position)
        holder.textView.apply {
            setPadding(36, 24, 10, 36)
            text = student.shortName
            gravity = Gravity.CENTER
            textSize = 18f
        }
    }


    fun filterStudents(query: List<String>) {
        students = Students.filterStudents(query, _students)
        notifyDataSetChanged()
    }

    private fun getItem(position: Int): Student {
        return students[position]
    }

    override fun getItemCount() = students.size
}
