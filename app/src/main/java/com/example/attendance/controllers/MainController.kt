package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.createAdapter
import com.example.attendance.models.students
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.UserLoader
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.fragment_main_content.*
import kotlinx.serialization.UnstableDefault

@UnstableDefault
object MainController : FragmentController {
    private lateinit var context: Fragment
    private var constraints = listOf<String>()

    override fun init(context: Fragment) {
        MainController.context = context
        with(context) {
            val classListAdapter = createAdapter(students)
            classListAdapter.filterStudents(constraints)
            classListView.adapter = classListAdapter
            FirebaseAuth.getInstance()
                .addAuthStateListener {
                    if (it.currentUser != null) {
                        val user = UserLoader.getUser()
                        Snackbar.make(parentView, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }
            if (FirebaseAuth.getInstance().currentUser == null) {
                Snackbar.make(parentView, "You are not signed in", Snackbar.LENGTH_INDEFINITE)
                    .apply {
                        setAction("Sign In") {
                            Navigation.navigate(R.id.signInFragment)
                        }
                        show()
                    }
            }
            filter.setOnClickListener {
                Navigation.navigate(R.id.filterFragment)
            }
        }
    }


    fun updateFilters(constraints: String) {
        this.constraints = constraints.split(" ")
    }

    override fun restoreState() {
    }
}
