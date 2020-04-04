package com.example.attendance.controllers

import android.graphics.Color
import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistPagerAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.util.android.Navigation
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

@UnstableDefault
object ClasslistController : FragmentController() {
    lateinit var attendance: Attendance
    private lateinit var callback: () -> Unit

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            toolbarClasslistToolbar.apply {
                setNavigationIcon(R.drawable.ic_baseline_close_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    Navigation.navigate(R.id.attendanceFragment)
                }
            }
        }
        if (::callback.isInitialized)
            callback()
    }


    fun setClasslist(attendance: Attendance) {
        val run = {
            this.attendance = attendance
            with(context) {
                toolbarClasslistToolbar.title = attendance.name
//                classListView.adapter = ClasslistAdapter(
//                    Students.filterStudents(attendance.constraints.split(" "))
//                        .map {
//                            StatefulStudent(it, 0)
//                        }, true)
                classlistViewPager.adapter = ClasslistPagerAdapter(attendance, this)
            }
        }
        if (contextInitialized() && context.view != null) run()
        else callback = run
    }


}
