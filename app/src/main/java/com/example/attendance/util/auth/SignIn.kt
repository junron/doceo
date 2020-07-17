package com.example.attendance.util.auth

import android.app.Activity
import android.widget.Toast
import com.auth0.android.jwt.JWT
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.Volley.queue
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.models.CSR
import com.example.attendance.util.auth.models.SignedCertificateWithToken
import com.example.attendance.util.postJson
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthCredential
import com.google.firebase.auth.OAuthProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json


@UnstableDefault
object SignIn {
    private const val tenantId = "d72a7172-d5f8-4889-9a85-d7424751592a"

    private fun signInUser(activity: Activity, callback: (token: String) -> Unit) {
        val provider = OAuthProvider.newBuilder("microsoft.com")
        val params = mapOf("tenant" to tenantId)
        provider.addCustomParameters(params)
        val result = FirebaseAuth.getInstance().pendingAuthResult ?: FirebaseAuth.getInstance()
            .startActivityForSignInWithProvider(activity, provider.build())
        result.addOnSuccessListener {
            val token = (it.credential as OAuthCredential).idToken
            callback(token)
        }
            .addOnFailureListener {
                Toast.makeText(
                    activity,
                    "Error signing in. Check your internet connection.",
                    Toast.LENGTH_LONG
                ).show()
            }
    }

    fun startSignIn(activity: Activity, callback: () -> Unit) {
        signInUser(activity) { token ->
            val claims = JWT(token).claims
            val name = claims["name"]?.asString() ?: return@signInUser
            val id = claims["email"]?.asString() ?: return@signInUser
            val csr = Crypto().generateCSR(activity, name, id, token)
            getSignedCertificate(csr) {
                UserLoader.setUserCredentials(it, token)
                Navigation.navigate(R.id.attendanceFragment)
                MainActivity.activity.recreate()
                callback()
            }
        }
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
