package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.auth.User
import kotlinx.android.synthetic.main.fragment_nearby.*

object NearbyController : FragmentController() {
    private var advertising = false
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
            toggleAdvertise.setOnClickListener {
                advertising = if (!advertising) {
                    AndroidNearby.startAdvertising()
                    toggleAdvertise.text = "Stop"
                    true
                } else {
                    AndroidNearby.stopAdvertising()
                    toggleAdvertise.text = "Start"
                    false
                }
            }
        }
    }

    fun onConnected(user: User) {
        println("Connected to $user")
    }

    fun onHandshakeComplete() {
        println("handshake complete")
    }

    fun onDisconnected() {
    }
}
