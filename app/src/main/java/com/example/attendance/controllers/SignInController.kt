package com.example.attendance.controllers

import androidx.fragment.app.Fragment
import com.auth0.android.jwt.JWT
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.auth.Crypto
import com.example.attendance.util.auth.SignIn
import com.example.attendance.util.auth.UserLoader
import kotlinx.android.synthetic.main.fragment_sign_in.*
import kotlinx.serialization.UnstableDefault

object SignInController : FragmentController() {

    @UnstableDefault
    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            SignIn.signInUser(signinWebview) { token ->
                val claims = JWT(token).claims
                val name = claims["given_name"]?.asString() ?: return@signInUser
                val id = claims["unique_name"]?.asString() ?: return@signInUser
                val csr = Crypto().generateCSR(context.context!!, name, id, token)
                SignIn.getSignedCertificate(csr) {
                    UserLoader.setUserCredentials(it, token)
                    UserLoader.loadFirebaseUser { error ->
                        println("FirebaseAuthError: $error")
                    }
                    Navigation.navigate(R.id.mainContent)
                    (activity as MainActivity).recreate()
                }
            }
        }
    }
}
