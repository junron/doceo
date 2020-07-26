package com.example.attendance.controllers

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.attendance.StudentChip
import com.example.attendance.models.Student
import com.example.attendance.models.Students
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.hideKeyboard
import com.pchmn.materialchips.ChipsInput
import com.pchmn.materialchips.model.ChipInterface
import kotlinx.android.synthetic.main.fragment_student_select.*

object StudentSelectController : FragmentController() {
    lateinit var initializeSetup: () -> Unit
    fun initializeSharing(
        exclude: List<String> = emptyList(),
        back: Int,
        callback: (selected: List<Student>, editing: Boolean) -> Unit
    ) {
        val setup = {
            MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            with(context) {
                var edit = true
                selectEditToggle.visibility = View.VISIBLE
                studentSelectDone.visibility = View.VISIBLE
                selectEditToggle.setOnClickListener {
                    edit = edit xor true
                    if (edit) {
                        selectEditToggle.setImageResource(R.drawable.ic_baseline_edit_24)
                    } else {
                        selectEditToggle.setImageResource(R.drawable.ic_eye_24)
                    }
                }

                studentFilter.filterableList = Students.students.filter { it.id !in exclude }.map {
                    StudentChip(
                        context!!,
                        it
                    )
                }
                studentFilter.addChipsListener(object : ChipsInput.ChipsListener {
                    override fun onChipAdded(chip: ChipInterface?, newSize: Int) {
                    }

                    override fun onChipRemoved(chip: ChipInterface?, newSize: Int) {
                    }

                    override fun onTextChanged(text: CharSequence) {
                        studentSelectDone.visibility =
                            if (text.isEmpty()) View.VISIBLE
                            else View.GONE
                    }

                })

                studentSelectDone.setOnClickListener {
                    Navigation.navigate(back)
                    hideKeyboard(activity!!)
                    callback(
                        studentFilter.selectedChipList.map { Students.getStudentByEmail(it.id.toString())!! },
                        edit
                    )
                }
            }
        }
        if (contextInitialized() && context.view != null) {
            setup()
        } else {
            initializeSetup = setup
        }

    }


    override fun onViewCreated() {
        super.onViewCreated()
        initializeSetup()
    }
}
