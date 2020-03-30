package com.example.attendance

import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import com.example.attendance.models.AttendanceLoader
import com.example.attendance.models.Students
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
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.view.*
import kotlinx.android.synthetic.main.side_navigation.view.*
import kotlinx.serialization.UnstableDefault

class MainActivity : AppCompatActivity() {
    companion object {
        lateinit var drawerLayout: DrawerLayout
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
                R.id.nav_attendance to R.id.attendanceFragment
            ), navController, drawerLayout.sideNavView
        )
        Students.loadStudents(this)
        createNotificationChannel(this, "NUSH Attendance", "NUS High attendance")
        Volley.init(this)
        AndroidNearby.init(this)
        UserLoader.context = this
        initNavigationHandlers()
        setThemeIcon()
        if (!UserLoader.userExists()) return
        Handshake.init(this)
        NotificationServer.init()
        AttendanceLoader.setup()
    }


    fun initNavigationHandlers() {
        if (UserLoader.userExists()) {
            drawerLayout.sideNavView.getHeaderView(0).username.text =
                UserLoader.getUser().name
            with(drawerLayout.sideNavView.menu) {
                val item = getItem(1)
                item.title = SpannableString("Logout").apply {
                    setSpan(ForegroundColorSpan(Color.RED), 0, 6, 0)
                }
                item.setOnMenuItemClickListener {
                    UserLoader.destroyCredentials()
                    FirebaseAuth.getInstance().signOut()
                    drawerLayout.closeDrawer(Gravity.LEFT)
                    this@MainActivity.recreate()
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

    private fun setThemeIcon() {
        if (Preferences.isDarkMode()) {
            darkModeToggle.setImageResource(R.drawable.ic_dark_mode)
        } else {
            darkModeToggle.setImageResource(R.drawable.ic_light_mode)
        }
    }
}
