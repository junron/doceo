package com.example.attendance.adapters.attendance

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.controllers.MainController
import com.example.attendance.controllers.StudentSelectController
import com.example.attendance.models.AccessLevel
import com.example.attendance.models.ClasslistGroup
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.requestInputDialog
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.attendance_item.view.*
import kotlinx.android.synthetic.main.document_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_attendance.*

class AttendanceItemsAdapter(val fragment: Fragment, var data: List<ClasslistGroup>) :
    BaseAdapter() {
    private val user = UserLoader.getUser().email

    @ExperimentalStdlibApi
    override fun getView(p0: Int, p1: View?, parent: ViewGroup): View {
        val item = getItem(p0)
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.attendance_item, null)
        with(view) {
            setOnClickListener {
                item.opened()
                ClasslistController.setClasslistGroup(item)
                Navigation.navigate(R.id.mainContent)
            }
            itemName.text = item.name
            lastOpenTime.text = item.lastOpenedDate()
            if (item.permissions.size > 1) {
                itemShared.visibility = View.VISIBLE
            }
            itemMore.setOnClickListener {
                BottomSheetDialog(context)
                    .apply {
                        behavior.peekHeight = 1500
                        setContentView(
                            inflater.inflate(R.layout.document_bottom_sheet, null)
                                .apply {
                                    itemTitle.text = item.name
                                    itemA2hs.setOnClickListener {
                                        hide()
                                        item.addToHomeScreen()
                                    }
                                    itemDetails.setOnClickListener {
                                        hide()
                                        MainController.detailClasslistGroup = item
                                        MainController.updateDetails(item)
                                        fragment.detailsClose.setOnClickListener {
                                            fragment.drawer_layout_end.closeDrawer(Gravity.RIGHT)
                                        }
                                        val adapter =
                                            PermissionsListAdapter(
                                                item,
                                                user
                                            )
                                        fragment.permissionsListView.adapter = adapter
                                        fragment.drawer_layout_end.openDrawer(Gravity.RIGHT)
                                    }
                                    if (item.getAccessLevel(user) != AccessLevel.OWNER) {
                                        itemRename.visibility = View.GONE
                                        itemRemove.visibility = View.GONE
                                        itemShare.visibility = View.GONE
                                        return@apply
                                    }
                                    itemRename.setOnClickListener {
                                        hide()
                                        context.requestInputDialog(
                                            "Rename item",
                                            item.name,
                                            "Rename"
                                        ) {
                                            it ?: return@requestInputDialog
                                            item.rename(it)
                                        }
                                    }
                                    itemRemove.setOnClickListener {
                                        hide()
                                        item.delete()
                                        Snackbar.make(
                                            fragment.drawer_layout_end,
                                            "Item removed",
                                            Snackbar.LENGTH_LONG
                                        ).setAction("Undo") {
                                            item.restore()
                                            Snackbar.make(
                                                fragment.drawer_layout_end,
                                                "Item restored",
                                                Snackbar.LENGTH_SHORT
                                            ).show()
                                        }.show()
                                    }
                                    itemShare.setOnClickListener {
                                        hide()
                                        Navigation.navigate(R.id.studentSelectFragment)
                                        StudentSelectController.initializeSharing(
                                            exclude = listOf(item.owner) + item.editors + item.viewers,
                                            back = R.id.attendanceFragment
                                        ) { selected, editing ->
                                            if (selected.isEmpty()) return@initializeSharing
                                            item.share(selected, editing)
                                        }
                                    }
                                })
                    }
                    .show()

            }
        }
        return view
    }

    override fun getItem(p0: Int) = data[p0]

    override fun getItemId(p0: Int) = 0L

    override fun getCount() = data.size
}
