package com.example.attendance.controllers

import android.graphics.Color
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.TagAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.models.Students
import com.example.attendance.models.Tags
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.onTextChange
import kotlinx.android.synthetic.main.fragment_new_attendance.*

object NewClasslistController : FragmentController() {
    private var name: String? = null
    private var constraints: String? = null
    private var tags = Tags.defaultTags
    private val adapter = TagAdapter(Tags.defaultTags.toMutableList(), true)
    override fun init(context: Fragment) {
        super.init(context)
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        with(context) {
            newClasslistToolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_close_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    Navigation.navigate(R.id.attendanceFragment)
                }
            }
            selectStudents.setOnClickListener {
                name = classlistName.editText?.text.toString()
                tags = adapter.tags
                constraints?.let {
                    FilterController.constraints = it
                }
                FilterController.callback = {
                    Navigation.navigate(R.id.newAttendance2)
                    constraints = it ?: constraints
                }
                Navigation.navigate(R.id.filterFragment)
            }
            classlistName.editText?.onTextChange {
                checkValidState()
            }
            tagList.adapter = adapter
            classlistDone.setOnClickListener {
                Attendance.newAttendance(
                    classlistName.editText?.text.toString(),
                    adapter.tags.filter { it.color != -1 },
                    constraints!!
                )
                Navigation.navigate(R.id.attendanceFragment)
                constraints = null
                tags = Tags.defaultTags
                name = null
            }
        }
    }

    fun checkValidState() {
        with(context) {
            if (classlistName.editText?.text.toString().isNotBlank())
                if (adapter.tags.size > 1)
                    if (constraints != null) {
                        classlistDone.visibility = View.VISIBLE
                        return@with
                    }
            classlistDone.visibility = View.GONE
        }
    }

    override fun restoreState() {
        super.restoreState()
        with(context) {
            adapter.tags = tags.toMutableList()
            adapter.notifyDataSetChanged()
            classlistName.editText?.setText(name)
            val c = constraints
            if (c != null) {
                val students = Students.filterStudents(c.split(" "), Students.students)
                selectStudents.text = "${students.size} students selected"
            } else {
                selectStudents.text = "Select students"
            }
        }
    }
}
