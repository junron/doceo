package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.util.auth.SignIn
import kotlinx.android.synthetic.main.fragment_sign_in_now.*

object SignInNowController : FragmentController() {
    override fun init(context: Fragment) {
        super.init(context)
        context.signInButton.setOnClickListener {
            SignIn.startSignIn(context.activity as MainActivity, context.context!!)
        }
    }
}
