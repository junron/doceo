package com.example.attendance.controllers

import android.app.Activity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.adapters.snapmit.OnBoardingAdapter
import com.example.attendance.util.auth.SignIn
import kotlinx.android.synthetic.main.fragment_login.*
import java.util.*
import kotlin.concurrent.fixedRateTimer

object SignInNowController : FragmentController() {
    private lateinit var scrollTimer: Timer

    private lateinit var activity: Activity

    override fun init(context: Fragment) {
        super.init(context)
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
        activity = context.requireActivity()
        with(context) {
            login_microsoft.setOnClickListener {
                SignIn.startSignIn(this@SignInNowController.activity) {
                    login_microsoft.isEnabled = true
                    login_microsoft.text = "Sign in"
                    scrollTimer.cancel()
                }
                login_microsoft.isEnabled = false
                login_microsoft.text = "Signing in..."
            }
            viewpager.adapter =
                OnBoardingAdapter()
            viewpager.isUserInputEnabled = false
            viewpager.offscreenPageLimit = 1
            scrollTimer = fixedRateTimer("viewpagerScroller", false, period = 5000) {
                this@SignInNowController.activity.runOnUiThread {
                    context.view ?: return@runOnUiThread
                    viewpager.setCurrentItem(
                        (viewpager.currentItem + 1) % viewpager.adapter!!.itemCount,
                        true
                    )
                }
            }
        }
    }
}
