package com.example.attendance.adapters

import android.view.Gravity
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.models.AccessLevel
import com.example.attendance.models.ClasslistInstance
import com.example.attendance.models.StatefulStudent
import com.example.attendance.util.android.showIcons
import com.example.attendance.util.auth.UserLoader

class ClasslistAdapter(
    private var classlist: ClasslistInstance,
    var fullName: Boolean
) : RecyclerView.Adapter<ClasslistAdapter.StudentViewHolder>() {
    class StudentViewHolder(val textView: TextView) : RecyclerView.ViewHolder(textView)

    private var attendance = classlist.parent!!
    lateinit var students: List<StatefulStudent>
    private val user = UserLoader.getUser()

    init {
        dataChanged(classlist)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        StudentViewHolder(TextView(parent.context))

    override fun onBindViewHolder(holder: StudentViewHolder, position: Int) {
        val (student, tag) = getItem(position)
        holder.textView.apply {
            text = if (fullName) student.name else student.shortName
            setTextColor(tag.color)
            setPadding(36, 24, 10, 36)
            gravity = Gravity.CENTER
            textSize = 18f
            if (attendance.getAccessLevel(user.email) == AccessLevel.VIEWER) return@apply
            setOnClickListener {
                classlist.setStudentState(
                    student,
                    if (tag == attendance.getParsedTags().first()) attendance.getParsedTags()[1]
                    else attendance.getParsedTags()[0]
                )
            }
            setOnLongClickListener {
                PopupMenu(context, it, Gravity.TOP).apply {
                    menu.apply {
                        attendance.getParsedTags().drop(2).forEach { tag ->
                            menu.add(tag.name).icon =
                                context.getDrawable(R.drawable.ic_circle)?.apply {
                                    setTint(tag.color)
                                }
                        }
                        setOnMenuItemClickListener { item ->
                            val selectedTag =
                                attendance.getParsedTags().find { tag -> tag.name == item.title }
                                    ?: return@setOnMenuItemClickListener false
                            classlist.setStudentState(
                                student,
                                selectedTag
                            )
                            true
                        }
                    }
                }.apply {
                    showIcons()
                    show()
                }
                true
            }
        }
    }

    fun dataChanged(classlist: ClasslistInstance) {
        this.classlist = classlist
        this.attendance = classlist.parent!!
        val defaultTag = attendance.getParsedTags().first()
        val tags = attendance.getParsedTags().map { it.id to it }.toMap()
        students = attendance.students.map {
            StatefulStudent(it, tags[classlist.studentState[it.id]] ?: defaultTag)
        }
        notifyDataSetChanged()
    }

    private fun getItem(position: Int) = students[position]

    override fun getItemCount() = students.size
}
