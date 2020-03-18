package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.adapters.createAdapter
import com.example.attendance.models.students
import kotlinx.android.synthetic.main.fragment_main_content.*

object MainController : FragmentController {
    private lateinit var context: Fragment

    override fun init(context: Fragment) {
        MainController.context = context
        with(context) {
            toolbar.title = "Attendance"
            classListView.adapter = createAdapter(students)
        }
    }

    override fun restoreState() {
    }
}
