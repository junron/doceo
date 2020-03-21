package com.example.attendance.controllers

import android.Manifest
import androidx.fragment.app.Fragment
import com.example.attendance.util.android.nearby.AndroidNearby
import com.karumi.dexter.Dexter
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.single.BasePermissionListener
import kotlinx.android.synthetic.main.fragment_nearby.*

object NearbyController : FragmentController() {
    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            advertise.setOnClickListener {
                AndroidNearby.startAdvertising()
            }
            discover.setOnClickListener {
                Dexter.withActivity(context.activity)
                    .withPermission(Manifest.permission.ACCESS_COARSE_LOCATION)
                    .withListener(object : BasePermissionListener() {
                        override fun onPermissionGranted(response: PermissionGrantedResponse?) {
                            AndroidNearby.startDiscovery()
                        }
                    })
                    .check()
            }
            stop.setOnClickListener {
                AndroidNearby.stop()
            }
        }
    }
}
