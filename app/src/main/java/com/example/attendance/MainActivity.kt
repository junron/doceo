package com.example.attendance

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.view.Gravity
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.fragment.findNavController
import com.example.attendance.models.loadStudents
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.auth.SignIn
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
        updateTextScale()

        val navController = nav_host_fragment.findNavController()
        Navigation.init(
            mapOf(
                R.id.nav_main to R.id.mainContent,
                R.id.nav_events to R.id.nearbyFragment
            ), navController, bottomAppBarNav
        )
        drawerLayout = drawer_layout
        loadStudents(this)
        SignIn.init(this)
        AndroidNearby.init(this)
        UserLoader.context = this
        initNavigationHandlers()
        setThemeIcon()
    }

    fun toggleDarkMode(dark: Boolean) {
        println("Dark theme? $dark")
        Preferences.setDarkMode(dark)
        this.recreate()
    }

    private fun updateTextScale() {
        val metrics = resources.displayMetrics
        val wm =
            getSystemService(Context.WINDOW_SERVICE) as WindowManager
        wm.defaultDisplay.getMetrics(metrics)
        metrics.scaledDensity =
            applicationContext.resources.configuration.fontScale * metrics.density
        baseContext.resources.updateConfiguration(
            applicationContext.resources.configuration,
            metrics
        )
    }

    fun setFontScale(scale: Float) {
        Preferences.setTextScale(scale)
        this.recreate()
    }

    override fun attachBaseContext(newBase: Context) {
        Preferences.init(newBase)
        val config = newBase.resources.configuration
        config.fontScale = Preferences.getTextScale()
        val newContext = newBase.createConfigurationContext(config)
        super.attachBaseContext(newContext)
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
        } else {
            with(drawerLayout.sideNavView.menu) {
                val item = getItem(1)
                item.title = "Sign in"
                item.setOnMenuItemClickListener {
                    Navigation.navigate(R.id.signInFragment)
                    drawerLayout.closeDrawer(Gravity.LEFT)
                    true
                }
            }
        }
        settingsButton.setOnClickListener {
            drawerLayout.closeDrawer(Gravity.LEFT)
            Navigation.navigate(R.id.settings)
        }
        darkModeToggle.setOnClickListener {
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
