package com.example.attendance.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.models.AccessLevel
import com.example.attendance.models.Attendance
import com.example.attendance.util.android.requestInputDialog
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.attendance_item.view.*
import kotlinx.android.synthetic.main.document_bottom_sheet.view.*
import kotlinx.android.synthetic.main.fragment_attendance.*

class AttendanceItemsAdapter(val fragment: Fragment, var data: List<Attendance>) : BaseAdapter() {
    override fun getView(p0: Int, p1: View?, parent: ViewGroup): View {
        val item = getItem(p0)
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.attendance_item, null)
        with(view) {
            itemName.text = item.name
            itemMore.setOnClickListener {
                BottomSheetDialog(context)
                    .apply {
                        behavior.peekHeight = 800
                        setContentView(inflater.inflate(R.layout.document_bottom_sheet, null)
                            .apply {
                                itemTitle.text = item.name
                                itemDetails.setOnClickListener {
                                    hide()
                                    fragment.itemDetailsTitle.text = item.name
                                    fragment.detailsCreatedTime.text = item.getCreatedTime()
                                    fragment.detailsModifiedTime.text = item.getModifiedTime()
                                    fragment.drawer_layout_end.openDrawer(Gravity.RIGHT)
                                }
                                if (item.getAccessLevel(UserLoader.getUser().email) != AccessLevel.OWNER) {
                                    itemRename.visibility = View.GONE
                                    itemRemove.visibility = View.GONE
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