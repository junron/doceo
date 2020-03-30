package com.example.attendance.controllers

import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.Volley
import com.example.attendance.util.downloadFile
import kotlinx.android.synthetic.main.fragment_settings.*
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

object SettingsController : FragmentController() {

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
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
