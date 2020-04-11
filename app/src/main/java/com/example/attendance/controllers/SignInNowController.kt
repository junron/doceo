package com.example.attendance.controllers

import android.view.View
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.util.auth.SignIn
import kotlinx.android.synthetic.main.fragment_sign_in_now.*

object SignInNowController : FragmentController() {
    override fun init(context: Fragment) {
        super.init(context)
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        context.signInButton.setOnClickListener {
            context.signInInfo.visibility = View.GONE
            context.signinWebview.visibility = View.VISIBLE
            SignIn.startSignIn(context.signinWebview, context.context!!)
        }
    }
}
