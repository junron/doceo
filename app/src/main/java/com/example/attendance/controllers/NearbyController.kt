package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.auth.User
import kotlinx.android.synthetic.main.fragment_nearby.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.*
import kotlin.concurrent.fixedRateTimer

object NearbyController : FragmentController() {
    private var advertising = false
    private lateinit var timerTask: TimerTask
    private var seconds = 0

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
                    nearbyStatus.text = "Not connected"
                    stopAnimation()
                    toggleAdvertise.text = "Start"
                    false
                }
            }
        }
    }

    private fun startAnimation() {
        val animation =
            listOf(
                R.drawable.ic_cell_tower0,
                R.drawable.ic_cell_tower1,
                R.drawable.ic_cell_tower2,
                R.drawable.ic_cell_tower3
            )
        if (::timerTask.isInitialized) timerTask.cancel()
        fixedRateTimer("updateTime", false, 0L, 1000) {
            timerTask = this
            GlobalScope.launch(Dispatchers.Main) {
                context.cellTowerView.setImageResource(animation[seconds % 4])
            }
            seconds++
        }
    }

    private fun stopAnimation() {
        if (::timerTask.isInitialized) timerTask.cancel()
        seconds = 0
        context.cellTowerView.setImageResource(R.drawable.ic_cell_tower2)
    }

    fun startedAdvertising() {
        with(context) {
            nearbyStatus.text = "Searching..."
            startAnimation()
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
