package com.example.attendance.util.auth

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.RequestQueue
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.auth0.android.jwt.JWT
import com.example.attendance.AuthenticationActivity
import com.example.attendance.MainActivity
import com.example.attendance.R
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
    const val tenantId = "d72a7172-d5f8-4889-9a85-d7424751592a"
    const val clientId = "0dd4d56f-45c1-47e1-8f8c-7c93ec9c71aa"
    const val redirectUrl = "com.example.attendance.auth://callback"
    private lateinit var queue: RequestQueue

    fun init(context: Context) {
        queue = Volley.newRequestQueue(context)
    }

    fun startSignIn(activity: AppCompatActivity, context: Context) {
        signInUser(activity as MainActivity, context) { token ->
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
                activity.recreate()
            }
        }
    }

    fun signInUser(
        activity: MainActivity,
        context: Context,
        callback: (token: String) -> Unit
    ) {
        AuthenticationActivity.callback = {
            if (it != null) {
                getAccessToken(it, callback)
            }
        }
        activity.startActivity(Intent(context, AuthenticationActivity::class.java))
    }

    fun getAccessToken(code: String, callback: (token: String) -> Unit) {
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

    fun getSignedCertificate(
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
