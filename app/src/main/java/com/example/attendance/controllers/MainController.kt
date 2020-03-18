package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.createAdapter
import com.example.attendance.models.students
import com.example.attendance.util.android.Navigation
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main_content.*

object MainController : FragmentController {
    private lateinit var context: Fragment

    override fun init(context: Fragment) {
        MainController.context = context
        with(context) {
            classListView.adapter = createAdapter(students)
            Snackbar.make(parentView, "You are not signed in", Snackbar.LENGTH_LONG)
                .apply {
                    setAction("Sign In") {
                        Navigation.navigate(R.id.signInFragment)
                    }
                    show()
                }
        }
    }

    override fun restoreState() {
    }
}
