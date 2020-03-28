package com.example.attendance.controllers

import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.adapters.AttendanceItemsAdapter
import com.example.attendance.models.AttendanceLoader
import kotlinx.android.synthetic.main.fragment_attendance.*

object AttendanceListController : FragmentController() {
    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            drawer_layout_end.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val adapter = AttendanceItemsAdapter(
                context,
                AttendanceLoader.attendance.filter { attendance -> !attendance.deleted })
            attendanceItems.adapter = adapter
            AttendanceLoader.addListener {
                val data = it.filter { attendance -> !attendance.deleted }
                println("Updated data: $data")
                adapter.data = data
                adapter.notifyDataSetChanged()
            }
        }
    }
}
