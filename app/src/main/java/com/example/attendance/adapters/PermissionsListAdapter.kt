package com.example.attendance.adapters

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.widget.PopupMenu
import com.example.attendance.R
import com.example.attendance.models.AccessLevel.*
import com.example.attendance.models.Attendance
import com.example.attendance.models.AttendanceLoader
import com.example.attendance.models.Students
import com.example.attendance.models.colors
import com.example.attendance.util.android.showIcons
import kotlinx.android.synthetic.main.permission_item.view.*
import kotlin.math.abs

@ExperimentalStdlibApi
class PermissionsListAdapter(var attendance: Attendance, val currentUser: String) : BaseAdapter() {
    val id = attendance.id
    var permissions = attendance.permissions

    init {
        AttendanceLoader.addListener {
            attendance = it.find { item -> item.id == id } ?: return@addListener
            permissions = attendance.permissions
            notifyDataSetChanged()
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        val view = LayoutInflater.from(parent.context).inflate(R.layout.permission_item, null)
        val name = Students.getStudentById(item.uid)?.name ?: return view
        val initials = name.split(" ").take(2).fold("") { acc, it ->
            acc + it.first()
        }
        with(view) {
            permissionName.text = name
            permissionEmail.text = item.uid
            permissionIcon.text = initials
            permissionIcon.setBackgroundColor(getColor(name))
            when (item.accessLevel) {
                OWNER -> {
                    permissionOwner.visibility = VISIBLE
                    permissionsOther.visibility = GONE
                }
                EDITOR -> {
                    if (attendance.getAccessLevel(currentUser) != OWNER) return@with
                    permissionsOther.setOnClickListener {
                        PopupMenu(parent.context, permissionsOther)
                            .apply {
                                menuInflater.inflate(R.menu.permissions_menu, menu)
                                showIcons()
                                setOnMenuItemClickListener {
                                    handlePermissionChange(item.uid, it)
                                    true
                                }
                                show()
                            }
                    }
                }
                VIEWER -> {
                    permissionsOther.setImageResource(R.drawable.ic_eye_24)
                    if (attendance.getAccessLevel(currentUser) != OWNER) return@with
                    permissionsOther.setOnClickListener {
                        PopupMenu(parent.context, permissionsOther)
                            .apply {
                                menuInflater.inflate(R.menu.permissions_menu, menu)
                                showIcons()
                                setOnMenuItemClickListener {
                                    handlePermissionChange(item.uid, it)
                                    true
                                }
                                show()
                            }
                    }
                }
                NONE -> println("This shouldn't happen")
            }
        }
        return view
    }

    private fun handlePermissionChange(uid: String, item: MenuItem) {
        attendance.changePermissions(
            uid, when (item.itemId) {
                R.id.permissions_edit -> EDITOR
                R.id.permissions_view -> VIEWER
                else -> NONE
            }
        )
    }

    private fun getColor(name: String) = colors[abs(name.hashCode() % colors.size)]

    override fun getItem(position: Int) = permissions[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = permissions.size

}
