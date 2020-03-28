package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.adapters.ClasslistAdapter.Companion.createAdapter
import com.example.attendance.models.Students
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

@UnstableDefault
object MainController : FragmentController() {
    private var constraints = listOf<String>()
    private var signedIn = false

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
            val classListAdapter = createAdapter(Students.students)
            classListAdapter.filterStudents(constraints)
            classListView.adapter = classListAdapter
            if (!UserLoader.userExists()) {
                Snackbar.make(parentView, "You are not signed in", Snackbar.LENGTH_LONG)
                    .apply {
                        setAction("Sign In") {
                            Navigation.navigate(R.id.signInFragment)
                        }
                        show()
                    }
            } else if (!signedIn) {
                val user = UserLoader.getUser()
                signedIn = true
                Snackbar.make(parentView, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                    .show()
                (activity as MainActivity).initNavigationHandlers()
            }
            filter.setOnClickListener {
                Navigation.navigate(R.id.filterFragment)
            }
        }
    }


    fun updateFilters(constraints: String) {
        this.constraints = constraints.split(" ")
    }
}
