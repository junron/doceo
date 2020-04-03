package com.example.attendance.controllers

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.Volley
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.auth.UserLoader
import com.example.attendance.util.auth.models.SignedCertificateWithToken
import com.example.attendance.util.downloadFile
import com.example.attendance.util.suffix
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

object SettingsController : FragmentController() {
    private var taps = 0

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            toolbarSettings.setOnClickListener {
                taps++
                if (taps == 7) {
                    Toast.makeText(context.context!!, "You are now a developer", Toast.LENGTH_LONG)
                        .show()
                    taps = 0
                    Preferences.setDeveloper(true)
                    developerMode.visibility = View.VISIBLE
                } else {
                    Toast.makeText(
                        context.context!!,
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
            updateApp.setOnClickListener {
                val downloadUrl =
                    "https://junron-github-actions-public-artifacts.s3-ap-southeast-1.amazonaws.com/app-release.apk"
                GlobalScope.launch {
                    val file = Volley.queue.downloadFile(context.context!!.filesDir, downloadUrl)
                    val uri = FileProvider.getUriForFile(
                        context.context!!,
                        context.context!!.applicationContext.packageName + ".provider",
                        file
                    )
                    val install = Intent(Intent.ACTION_VIEW)
                    install.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    install.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    install.putExtra(Intent.EXTRA_NOT_UNKNOWN_SOURCE, true)
                    install.setDataAndType(uri, "application/vnd.android.package-archive")
                    context.startActivity(install)
                }
            }
            toolbarSettings.title = "Settings"
        }
    }
}
