package com.example.attendance.util.auth

import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import com.android.volley.toolbox.StringRequest
import com.auth0.android.jwt.JWT
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.Volley.queue
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.models.CSR
import com.example.attendance.util.auth.models.SignedCertificateWithToken
import com.example.attendance.util.postJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json


@UnstableDefault
object SignIn {
    private const val tenantId = "d72a7172-d5f8-4889-9a85-d7424751592a"
    private const val clientId = "0dd4d56f-45c1-47e1-8f8c-7c93ec9c71aa"
    private const val redirectUrl = "https://voting.nushhwboard.ml/callback"

    private fun signInUser(webview: WebView, callback: (token: String) -> Unit) {
        webview.webViewClient = object : WebViewClient() {
            override fun shouldOverrideUrlLoading(
                view: WebView?,
                request: WebResourceRequest?
            ): Boolean {
                if (request == null) return false
                if (request.url.toString().startsWith("$redirectUrl?code=")) {
                    val token = request.url.toString().substringAfter("code=").substringBefore("&")
                    getAccessToken(token, callback)
                }
                return true
            }
        }
        webview.settings.javaScriptEnabled = true
        webview.loadUrl("https://login.microsoft.com/$tenantId/oauth2/authorize?client_id=$clientId&response_type=code&redirect_uri=$redirectUrl")
    }

    fun startSignIn(webview: WebView, context: Context) {
        signInUser(webview) { token ->
            val claims = JWT(token).claims
            val name = claims["given_name"]?.asString() ?: return@signInUser
            val id = claims["unique_name"]?.asString() ?: return@signInUser
            val csr = Crypto().generateCSR(context, name, id, token)
            getSignedCertificate(csr) {
                UserLoader.setUserCredentials(it, token)
                UserLoader.loadFirebaseUser { error ->
                    println("FirebaseAuthError: $error")
                }
                Navigation.navigate(R.id.attendanceFragment)
                MainActivity.activity.recreate()
            }
        }
    }

    private fun getAccessToken(code: String, callback: (token: String) -> Unit) {
        queue.add(
            object : StringRequest(
                Method.POST,
                "https://login.microsoftonline.com/$tenantId/oauth2/token",
                { response ->
                    val token =
                        response.substringAfter("\"access_token\":\"").substringBefore("\"")
                    callback(token)
                },
                { error ->
                    println(String(error.networkResponse.data))
                    println("Error: $error")
                }
            ) {
                override fun getBody() = ("grant_type=authorization_code&" +
                        "client_id=$clientId&" +
                        "redirect_uri=$redirectUrl&" +
                        "code=$code").toByteArray()

                override fun getBodyContentType() = "application/x-www-form-urlencoded"
            }
        )
    }

    private fun getSignedCertificate(
        csr: CSR,
        callback: (certificate: SignedCertificateWithToken) -> Unit
    ) {
        GlobalScope.launch {
            val response = queue.postJson(
                "https://pyrostore.nushhwboard.ml/certificates/new",
                csr,
                CSR.serializer()
            )
            withContext(Dispatchers.Main) {
                callback(Json.parse(SignedCertificateWithToken.serializer(), response))
            }
        }
    }
}
