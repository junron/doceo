package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.AttendanceItemsAdapter
import com.example.attendance.models.Attendance
import com.example.attendance.models.AttendanceLoader
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_attendance.*

object AttendanceListController : FragmentController() {
    private var signedIn: Boolean = false
    var detailAttendance: Attendance? = null

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            toolbarMain.apply {
                setNavigationIcon(R.drawable.ic_baseline_menu_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    MainActivity.drawerLayout.openDrawer(Gravity.LEFT)
                }
            }
            if (!UserLoader.userExists()) {
                MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
                Navigation.navigate(R.id.signInNow)
                return@with
            } else if (!signedIn) {
                MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                val user = UserLoader.getUser()
                signedIn = true
                Snackbar.make(mainParent, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                    .show()
                (activity as MainActivity).initNavigationHandlers()
            }
            drawer_layout_end.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val adapter = AttendanceItemsAdapter(
                context,
                AttendanceLoader.attendance.filter { attendance -> !attendance.deleted })
            checkIfEmpty(AttendanceLoader.attendance.filter { attendance -> !attendance.deleted })
            attendanceItems.adapter = adapter
            AttendanceLoader.addListener {
                val data = it.filter { attendance -> !attendance.deleted }
                checkIfEmpty(data)
                ClasslistController.attendanceUpdated(data)
                adapter.data = data
                adapter.notifyDataSetChanged()
                if (detailAttendance != null) {
                    val item = data.find { item -> item.id == detailAttendance?.id }
                        ?: return@addListener kotlin.run {
                            detailAttendance = null
                        }
                    updateDetails(item)
                }
            }

            newAttendance.setOnClickListener {
                Navigation.navigate(R.id.newAttendance2)
            }
        }
    }

    fun updateDetails(item: Attendance) {
        if (context.view == null) return
        context.itemDetailsTitle.text = item.name
        context.detailsCreatedTime.text = item.getCreatedTime()
        context.detailsModifiedTime.text = item.getModifiedTime()
    }

    fun checkIfEmpty(attendance: List<Attendance>) {
        if (context.view == null) return
        if (attendance.isNotEmpty()) {
            context.attendanceItems.visibility = View.VISIBLE
            context.noClasslists.visibility = View.GONE
        } else {
            context.attendanceItems.visibility = View.GONE
            context.noClasslists.visibility = View.VISIBLE
        }
    }
}
