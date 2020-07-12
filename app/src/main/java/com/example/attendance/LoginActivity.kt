package com.example.attendance

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Window
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.attendance.adapters.OnBoardingAdapter
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_login.*
import java.util.*
import kotlin.concurrent.fixedRateTimer


class LoginActivity : AppCompatActivity() {
    var scrollTimer: Timer? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (FirebaseAuth.getInstance().currentUser != null) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        this.window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        setContentView(R.layout.activity_login)
        supportActionBar!!.hide()
        login_microsoft.setOnClickListener {
            val params = mapOf("tenant" to "d72a7172-d5f8-4889-9a85-d7424751592a")
            signIn(AuthUI.IdpConfig.MicrosoftBuilder().setCustomParameters(params).build())
        }
        viewpager.adapter = OnBoardingAdapter()
        scrollTimer = fixedRateTimer("viewpagerScroller", false, period = 5000) {
            runOnUiThread {
                viewpager.setCurrentItem(
                    (viewpager.currentItem + 1) % viewpager.adapter!!.itemCount,
                    true
                )
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scrollTimer?.cancel()
    }

    private fun signIn(authProvider: AuthUI.IdpConfig) {
        AuthUI.getInstance().signOut(this)
        startActivityForResult(
            AuthUI.getInstance()
                .createSignInIntentBuilder()
                .setAvailableProviders(listOf(authProvider))
                .setIsSmartLockEnabled(false)
                .build(),
            1
        )
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            val response = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK) {
                // Successfully signed in
                val user = FirebaseAuth.getInstance().currentUser
                startActivity(Intent(this, MainActivity::class.java))
                finish()
                // ...
            } else {
                if (response != null) Toast.makeText(
                    this,
                    "Error signing in. Check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
