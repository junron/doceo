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

    override fun init(context: Fragment) {
        MainController.context = context
        with(context) {
            classListView.adapter = createAdapter(students)
            FirebaseAuth.getInstance()
                .addAuthStateListener {
                    if (it.currentUser != null) {
                        val user = UserLoader.getUser()
                        Snackbar.make(parentView, "Welcome, ${user.name}!", Snackbar.LENGTH_LONG)
                            .show()
                    }
                }

            if (UserLoader.userExists()) {
                UserLoader.loadFirebaseUser {
                    println("Error: $it")
                }
            } else {
                Snackbar.make(parentView, "You are not signed in", Snackbar.LENGTH_INDEFINITE)
                    .apply {
                        setAction("Sign In") {
                            Navigation.navigate(R.id.signInFragment)
                        }
                        show()
                    }
            }
        }
    }

    override fun restoreState() {
    }
}
