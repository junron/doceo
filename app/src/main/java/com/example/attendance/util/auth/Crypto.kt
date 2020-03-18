package com.example.attendance.util.auth

import android.content.Context
import android.util.Base64
import com.example.attendance.util.auth.models.CSR
import com.example.attendance.util.auth.models.Certificate
import com.example.attendance.util.auth.models.SignedCertificate
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import org.whispersystems.curve25519.Curve25519
import java.security.SecureRandom

class Crypto {
    @UnstableDefault
    companion object {
        private val rootPublicKey = "VTw7JiXnuCIXDlsBZwCWyl53UFQcE5-l2QDNQKsCMjw=".loadKeyBytes()

        fun verifyCertificate(signedCertificate: SignedCertificate): Boolean {
            val message = Json.stringify(Certificate.serializer(), signedCertificate.certificate)
            return verify(message, signedCertificate.signature, rootPublicKey)
        }

        fun verify(message: String, signature: String, publicKey: ByteArray): Boolean {
            val cipher = Curve25519.getInstance(Curve25519.BEST)
            val signatureBytes = Base64.decode(signature, Base64.URL_SAFE)
            return cipher.verifySignature(publicKey, message.toByteArray(), signatureBytes)
        }

        fun sign(message: String, privateKey: ByteArray): String {
            val cipher = Curve25519.getInstance(Curve25519.BEST)
            return Base64.encodeToString(
                cipher.calculateSignature(privateKey, message.toByteArray()),
                Base64.URL_SAFE
            )
        }

        fun randomString(): String {
            val bytes = ByteArray(32)
            SecureRandom().nextBytes(bytes)
            return String(bytes)
        }

        fun String.loadKeyBytes(): ByteArray =
            Base64.decode(this, Base64.URL_SAFE)
    }

    private lateinit var privateKey: ByteArray
    private lateinit var publicKey: ByteArray


    fun loadKeysFromFile(context: Context) {
        publicKey = context.filesDir.resolve("keys/public.key").readText().loadKeyBytes()
        privateKey = context.filesDir.resolve("keys/private.key").readText().loadKeyBytes()
    }

    private fun generateKeyPair(context: Context) {
        val instance = Curve25519.getInstance(Curve25519.BEST)
        val keyPair = instance.generateKeyPair()
        this.privateKey = keyPair.privateKey
        this.publicKey = keyPair.publicKey
        context.filesDir.resolve("keys").mkdir()
        context.filesDir.resolve("keys/private.key").apply {
            createNewFile()
            writeText(Base64.encodeToString(keyPair.privateKey, Base64.URL_SAFE))
        }
        context.filesDir.resolve("keys/public.key").apply {
            createNewFile()
            writeText(Base64.encodeToString(keyPair.publicKey, Base64.URL_SAFE))
        }
    }

    fun generateCSR(context: Context, name: String, id: String, token: String): CSR {
        generateKeyPair(context)
        return CSR(Base64.encodeToString(publicKey, Base64.URL_SAFE), name, id, token)
    }


    fun sign(message: String) = sign(message, privateKey)

}
