package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.models.Students
import com.example.attendance.util.android.Navigation
import kotlinx.android.synthetic.main.fragment_new_attendance.*

object NewClasslistController : FragmentController() {
    private var name: String? = null
    private var constraints: String? = null
    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            selectStudents.setOnClickListener {
                name = classlistName.editText?.text.toString()
                constraints?.let {
                    FilterController.constraints = it
                }
                FilterController.callback = {
                    Navigation.navigate(R.id.newAttendance2)
                    constraints = it
                }
                Navigation.navigate(R.id.filterFragment)
            }
        }
    }

    override fun restoreState() {
        super.restoreState()
        println(name)
        with(context) {
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
