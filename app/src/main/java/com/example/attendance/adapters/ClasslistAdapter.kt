package com.example.attendance.adapters

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.models.ClasslistEvent
import com.example.attendance.models.ClasslistInstance
import com.example.attendance.models.StatefulStudent
import com.example.attendance.models.Students

class ClasslistAdapter(
    private var classlist: ClasslistInstance
) : RecyclerView.Adapter<ClasslistAdapter.StudentViewHolder>() {
    class StudentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    private var attendance = classlist.parent!!
    lateinit var students: List<StatefulStudent>

    init {
        dataChanged(classlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudentViewHolder(TextView(parent.context))

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val (student, tag) = getItem(position)
        holder.textView.apply {
            text = student.shortName
            setTextColor(tag.color)
            setPadding(36, 24, 10, 36)
            setOnClickListener {
                classlist.addEvent(
                    ClasslistEvent.StateChanged(
                        student.id,
                        (if (tag == attendance.getParsedTags()
                                .first()
                        ) attendance.getParsedTags()[1]
                        else attendance.getParsedTags()[0]).id
                    )
                )
            }
            gravity = Gravity.CENTER
            textSize = 18f
        }
    }

    fun dataChanged(classlist: ClasslistInstance) {
        this.classlist = classlist
        this.attendance = classlist.parent!!
        val defaultTag = attendance.getParsedTags().first()
        val tags = attendance.getParsedTags().map { it.id to it }.toMap()
        val stateMap = mutableMapOf<String, String>()
        classlist.getParsedEvents().forEach {
            if (it is ClasslistEvent.StateChanged) {
                stateMap[it.targetId] = it.state
            }
        }
        students = Students.filterStudents(attendance.constraints.split(" "))
            .map { StatefulStudent(it, tags[stateMap[it.id]] ?: defaultTag) }
        notifyDataSetChanged()
    }

    private fun getItem(position: Int) = students[position]

    override fun getItemCount() = students.size
}
