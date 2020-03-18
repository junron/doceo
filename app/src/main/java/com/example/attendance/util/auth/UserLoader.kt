package com.example.attendance.util.auth

import android.content.Context
import com.example.attendance.util.auth.models.SignedCertificate
import com.example.attendance.util.auth.models.SignedCertificateWithToken
import com.google.firebase.auth.FirebaseAuth
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
object UserLoader {
    lateinit var context: Context

    fun setUserCredentials(signedData: SignedCertificateWithToken) {
        val (certificate, token) = signedData
        if (!Crypto.verifyCertificate(certificate)) throw IllegalStateException("Certificate signature invalid")
        with(context) {
            filesDir.resolve("user").mkdir()
            with(filesDir.resolve("user/certificate")) {
                createNewFile()
                writeText(Json.stringify(SignedCertificate.serializer(), certificate))
            }
            with(filesDir.resolve("user/token")) {
                createNewFile()
                writeText(token)
            }
        }
    }

    fun loadFirebaseUser(failure: (String) -> Unit) {
        with(context) {
            val token = filesDir.resolve("user/token").readText()
            FirebaseAuth.getInstance().signInWithCustomToken(token)
                .addOnFailureListener {
                    failure(it.message.toString())
                }
        }
    }

    fun getCertificate() = Json.parse(
        SignedCertificate.serializer(),
        context.filesDir.resolve("user/certificate").readText()
    )

    fun getUser() = with(getCertificate()) {
        User(certificate.name, certificate.id, emptyMap())
    }

    fun userExists() = context.filesDir.resolve("user/token").exists()

    fun destroyCredentials() {
        context.filesDir.resolve("user").deleteRecursively()
        FirebaseAuth.getInstance().signOut()
    }

}


data class User(val name: String, val email: String, val metadata: Map<String, String>)
