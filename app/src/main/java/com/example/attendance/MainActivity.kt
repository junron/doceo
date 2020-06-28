package com.example.attendance

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.camera2.Camera2Config
import androidx.camera.core.CameraXConfig
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.controllers.classlist.Camera
import com.example.attendance.models.ClasslistGroupLoader
import com.example.attendance.models.Students
import com.example.attendance.util.AppendOnlyStorage
import com.example.attendance.util.Volley
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.android.nearby.protocols.Handshake
import com.example.attendance.util.android.notifications.NotificationServer
import com.example.attendance.util.android.notifications.Notifications.createNotificationChannel
import com.example.attendance.util.auth.UserLoader
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.iid.FirebaseInstanceId
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.side_navigation.view.*
import kotlinx.serialization.UnstableDefault

@SuppressLint("RtlHardcoded")
class MainActivity : AppCompatActivity(), CameraXConfig.Provider {
    companion object {
        lateinit var drawerLayout: DrawerLayout
        lateinit var activity: MainActivity
    }

    @UnstableDefault
    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseApp.initializeApp(this)
        Preferences.init(this)
        setTheme(
            if (Preferences.isDarkMode()) R.style.AppTheme
            else R.style.AppTheme_Light
        )
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val navController = nav_host_fragment.findNavController()
        drawerLayout = drawer_layout
        Navigation.init(
            mapOf(
                R.id.nav_attendance to R.id.attendanceFragment,
                R.id.nav_nearby to R.id.nearbyFragment
            ), navController, drawerLayout.sideNavView
        )
        AppendOnlyStorage.init(this)
        Volley.init(this)
        UserLoader.context = this
        activity = this
        if (!UserLoader.userExists()) {
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            Navigation.navigate(R.id.signInNow)
            return
        }
        ClasslistGroupLoader.setup()
        Students.loadStudents(this)
        AndroidNearby.init(this)
        initNavigationHandlers()
        setThemeIcon()
        Handshake.init(this)
        createNotificationChannel(this, "NUSH Attendance", "NUS High attendance")
        NotificationServer.init()
        if (intent != null) handleIntents(intent)
    }

    @UnstableDefault
    private fun handleIntents(intent: Intent) {
        val id = intent.getStringExtra("attendance_id") ?: return
        val classlistGroup =
            ClasslistGroupLoader.classlistGroups.find { classlistGroup -> classlistGroup.id == id }
        if (classlistGroup != null) {
            classlistGroup.opened()
            ClasslistController.setClasslistGroup(classlistGroup)
            Navigation.navigate(R.id.mainContent)
            return
        }
        ClasslistGroupLoader.addListener {
            val classlistGroup =
                it.find { attendance -> attendance.id == id } ?: return@addListener run {
                    Toast.makeText(this, "Classlist could not be found.", Toast.LENGTH_LONG).show()
                }
            classlistGroup.opened()
            ClasslistController.setClasslistGroup(classlistGroup)
            Navigation.navigate(R.id.mainContent)
        }
    }

    @UnstableDefault
    fun initNavigationHandlers() {
        if (UserLoader.userExists()) {
            drawerLayout.sideNavView.getHeaderView(0).username.text =
                UserLoader.getUser().name
            with(drawerLayout.sideNavView.menu) {
                val item = getItem(this.size() - 1)
                item.title = SpannableString("Logout").apply {
                    setSpan(ForegroundColorSpan(Color.RED), 0, 6, 0)
                }
                item.setOnMenuItemClickListener {
                    UserLoader.destroyCredentials()
                    FirebaseAuth.getInstance().signOut()
                    drawerLayout.closeDrawer(Gravity.LEFT)
                    FirebaseInstanceId.getInstance().deleteInstanceId()
                    this@MainActivity.recreate()
                    Navigation.navigate(R.id.signInNow)
                    true
                }
            }
        }
        settingsButton.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.LEFT)
            Navigation.navigate(R.id.settings)
        }
        darkModeToggle.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.LEFT)
            Preferences.setDarkMode(!Preferences.isDarkMode())
            this@MainActivity.recreate()
        }
    }

    @UnstableDefault
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        intent ?: return
        handleIntents(intent)
    }

    private fun setThemeIcon() {
        if (Preferences.isDarkMode()) {
            darkModeToggle.setImageResource(R.drawable.ic_dark_mode)
        } else {
            darkModeToggle.setImageResource(R.drawable.ic_light_mode)
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            0 -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Camera.initCamera()
                }
            }
        }
    }

    override fun getCameraXConfig() = Camera2Config.defaultConfig()

    override fun onBackPressed() {
    }
}
