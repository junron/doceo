package com.example.attendance.controllers

import android.Manifest
import android.view.View.GONE
import androidx.fragment.app.Fragment
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.android.nearby.NearbyMessage
import com.example.attendance.util.android.nearby.NearbyStage
import com.example.attendance.util.android.nearby.state
import com.example.attendance.util.android.plusAssign
import com.example.attendance.util.auth.UserLoader.getUserOrNull
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.fragment_nearby.*

object NearbyController : FragmentController() {
    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            val user = getUserOrNull() ?: return@with
            if (user.isMentorRep) {
                advertise.setOnClickListener {
                    AndroidNearby.startAdvertising()
                    advertise.isEnabled = false
                }
                discover.visibility = GONE
            } else {
                discover.setOnClickListener {
                    Dexter.withActivity(context.activity)
                        .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                        .withListener(object : BasePermissionListener() {
                            override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                                AndroidNearby.startDiscovery()
                            }
                        })
                        .check()
                    discover.isEnabled = false
                }
                advertise.visibility = GONE
            }

            stop.setOnClickListener {
                AndroidNearby.stop()
                discover.isEnabled = true
                advertise.isEnabled = true
            }
            send.setOnClickListener {
                val text = messageBox.text.toString()
                state.values.first().sendPayload(
                    NearbyMessage(
                        NearbyStage.GENERIC_DATA, text
                    ).toPayload()
                )
            }
        }
    }

    fun dataMessageReceived(data: String) {
        context.nearbyOutput += "$data\n"
    }

    fun handshakeEventReceived(data: String) {
        context.nearbyOutput += "Handshake: $data\n"
    }

    fun onConnected() {
        AndroidNearby.stopDiscovery()
        context.send.isEnabled = true
    }

    fun onDisconnected() {
        context.send.isEnabled = false
    }
}
