package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.attendance.ClasslistGroupItemsAdapter
import com.example.attendance.fragments.MainFragment
import com.example.attendance.models.ClasslistGroup
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_attendance_list.*

object MainController : FragmentController() {
    private var signedIn: Boolean = false
    var detailClasslistGroup: ClasslistGroup? = null

    override fun init(context: Fragment) {
        super.init(context)
        context as MainFragment
        with(context) {
            toolbarMain.apply {
                setNavigationIcon(R.drawable.ic_baseline_menu_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    MainActivity.drawerLayout.openDrawer(Gravity.LEFT)
                }
            }
            MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
            if (!signedIn) {
                val user = UserLoader.getUser()
                signedIn = true
                Snackbar.make(mainParent, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                    .show()
                (activity as MainActivity).initNavigationHandlers()
            }
            drawer_layout_end.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            val classlistGroups = viewModel.classlistGroups.value
            val adapter =
                ClasslistGroupItemsAdapter(
                    context,
                    classlistGroups.filter { attendance -> !attendance.deleted }
                        .sortedByDescending { attendance -> attendance.getLastAccess() })
            checkIfEmpty(classlistGroups.filter { attendance -> !attendance.deleted })
            attendanceItems.adapter = adapter
            viewModel.classlistGroups.observe({ lifecycle }) {
                println("Updated: $it")
                val data = it.filter { attendance -> !attendance.deleted }
                checkIfEmpty(data)
                adapter.data = data.sortedByDescending { attendance -> attendance.getLastAccess() }
                adapter.notifyDataSetChanged()
                if (detailClasslistGroup != null) {
                    val item = data.find { item -> item.id == detailClasslistGroup?.id }
                        ?: return@observe kotlin.run {
                            detailClasslistGroup = null
                        }
                    updateDetails(item)
                }
            }

            newAttendance.setOnClickListener {
                Navigation.navigate(R.id.newAttendance2)
            }
        }
    }

    fun updateDetails(item: ClasslistGroup) {
        if (context.view == null) return
        context.itemDetailsTitle.text = item.name
        context.detailsCreatedTime.text = item.getCreatedTime()
        context.detailsModifiedTime.text = item.getModifiedTime()
    }

    private fun checkIfEmpty(classlistGroup: List<ClasslistGroup>) {
        if (context.view == null) return
        if (classlistGroup.isNotEmpty()) {
            context.attendanceItems.visibility = View.VISIBLE
            context.noClasslists.visibility = View.GONE
        } else {
            context.attendanceItems.visibility = View.GONE
            context.noClasslists.visibility = View.VISIBLE
        }
    }
}
