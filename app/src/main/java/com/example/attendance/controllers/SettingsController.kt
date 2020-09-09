package com.example.attendance.controllers

import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.example.attendance.BuildConfig
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.auth.UserLoader
import com.example.attendance.util.auth.models.SignedCertificateWithToken
import com.example.attendance.util.suffix
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.serialization.json.Json

object SettingsController : FragmentController() {
    private var taps = 0

    override fun init(context: Fragment) {
        super.init(context)
        MainActivity.drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
        with(context) {
            val versionName = with(BuildConfig.VERSION_NAME) {
                if (endsWith("-p")) "Limited " + substringBefore("-p")
                else this
            }
            appVersion.text = versionName
            scanningSettings.setOnClickListener {
                Navigation.navigate(R.id.slideshowFragment)
            }
            toolbarSettings.setOnClickListener {
                taps++
                if (taps == 7) {
                    Toast.makeText(
                        context.requireContext(),
                        "You are now a developer",
                        Toast.LENGTH_LONG
                    )
                        .show()
                    taps = 0
                    Preferences.setDeveloper(true)
                    developerMode.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        context.requireContext(),
                        "You are ${7 - taps suffix "tap"} away from being a developer",
                        Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
            if (Preferences.getDeveloper()) {
                developerMode.visibility = View.VISIBLE
                sudo.setOnClickListener {
                    val (certString, privateKey, msToken) = sudoKeys.text.toString().split("||||")
                    val cert = Json.parse(
                        SignedCertificateWithToken.serializer(),
                        certString
                    )
                    val filesDir = context.context?.filesDir ?: return@setOnClickListener
                    UserLoader.setUserCredentials(cert, msToken)
                    filesDir.resolve("keys").mkdir()
                    filesDir.resolve("keys/private.key").writeText(privateKey)
                    filesDir.resolve("keys/public.key")
                        .writeText(cert.certificate.certificate.publicKey)
                    filesDir.resolve("user/token").writeText(cert.token)
                    UserLoader.loadFirebaseUser {
                        println("Failed: $it")
                    }
                    MainActivity.activity.recreate()
                }
            }
            toolbarSettings.apply {
                setNavigationIcon(R.drawable.ic_baseline_menu_24)
                navigationIcon?.setTint(Color.WHITE)
                setNavigationOnClickListener {
                    MainActivity.drawerLayout.openDrawer(Gravity.LEFT)
                }
            }
            toolbarSettings.title = "Settings"
        }
    }

}
