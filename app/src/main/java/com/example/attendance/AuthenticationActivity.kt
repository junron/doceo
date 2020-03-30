package com.example.attendance

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.browser.customtabs.CustomTabsIntent
import com.example.attendance.util.auth.SignIn

class AuthenticationActivity : AppCompatActivity() {
    companion object {
        var callback: ((String?) -> Unit)? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authorizationUrl =
            "https://login.microsoft.com/${SignIn.tenantId}/oauth2/authorize?client_id=${SignIn.clientId}&response_type=code&redirect_uri=com.example.attendance.auth://callback"

        val builder = CustomTabsIntent.Builder()
        val customTabsIntent = builder.build()
        customTabsIntent.launchUrl(
            this, Uri.parse(authorizationUrl)
        )
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val code = intent?.data?.getQueryParameter("code")
        callback?.invoke(code)
        finish()
    }
}
