package com.example.attendance.adapters

import android.content.Context
import android.net.Uri
import com.example.attendance.R
import com.example.attendance.models.Student
import com.pchmn.materialchips.model.ChipInterface

class StudentChip(val context: Context, private val student: Student) : ChipInterface {
    override fun getInfo() = student.id
    override fun getAvatarDrawable() = context.getDrawable(R.drawable.ic_baseline_account_circle_24)

    override fun getLabel() = student.name

    override fun getId() = student.id

    override fun getAvatarUri(): Uri =
        Uri.parse("android.resource://com.example.attendance/drawable/ic_baseline_account_circle_24")

}
