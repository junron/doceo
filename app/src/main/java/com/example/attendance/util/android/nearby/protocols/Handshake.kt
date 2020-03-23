package com.example.attendance.util.android.nearby.protocols

import android.content.Context
import com.example.attendance.controllers.NearbyController
import com.example.attendance.util.android.nearby.NearbyMessage
import com.example.attendance.util.android.nearby.NearbyStage
import com.example.attendance.util.auth.Crypto
import com.example.attendance.util.auth.Crypto.Companion.loadKeyBytes
import com.example.attendance.util.auth.UserLoader.getCertificate
import com.example.attendance.util.auth.models.SignedCertificate
import com.google.android.gms.nearby.connection.ConnectionsClient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class Handshake(connection: ConnectionsClient, endpointId: String) :
    Protocol(connection, endpointId) {
    /*
    Steps:
    0: Certificate exchange
    1: Receive and respond to challenge
    2: Receive and verify server response
     */
    lateinit var serverCertificate: SignedCertificate
    private lateinit var serverChallenge: String

    companion object {
        val clientCertificateString =
            Json.stringify(SignedCertificate.serializer(), getCertificate())
        val clientCrypto = Crypto()
        fun init(context: Context) {
            clientCrypto.loadKeysFromFile(context)
        }
    }

    private fun setServerCertificate(certificate: String): Boolean {
        return try {
            serverCertificate = Json.parse(SignedCertificate.serializer(), certificate)
            true
        } catch (error: SerializationException) {
            println("Invalid json: $certificate")
            false
        }
    }

    private fun generateServerChallenge(): String {
        // Generate random string to prove server has private key
        serverChallenge = Crypto.randomString()
        return serverChallenge
    }

    private fun respondToChallenge(randomString: String): String? {
        // Improperly seeded CSPRNGs
        if (randomString == serverChallenge) return null
        return try {
            clientCrypto.sign(randomString)
        } catch (e: Exception) {
            println("Signing exception: $e")
            null
        }
    }

    private fun verifyServerResponse(signature: String) = try {
        Crypto.verify(
            serverChallenge,
            signature,
            serverCertificate.certificate.publicKey.loadKeyBytes()
        )
    } catch (e: Exception) {
        println("Verification exception: $e")
        false
    }

    override fun next(message: NearbyMessage) {
        when (state) {
            0 -> {
                val result = setServerCertificate(message.data)
                if (!result) {
                    connection.disconnectFromEndpoint(endpointId)
                    return
                }
                //  Send challenge to server
                sendPayload(
                    NearbyMessage(
                        NearbyStage.HANDSHAKE,
                        generateServerChallenge()
                    ).toPayload()
                )
            }
            1 -> {
                val response = respondToChallenge(message.data) ?: kotlin.run {
                    connection.disconnectFromEndpoint(endpointId)
                    return
                }
                // Respond to server challenge
                sendPayload(
                    NearbyMessage(
                        NearbyStage.HANDSHAKE,
                        response
                    ).toPayload()
                )
            }
            2 -> {
                //  Verify server response
                if (!verifyServerResponse(message.data)) {
                    connection.disconnectFromEndpoint(endpointId)
                    return
                }
                NearbyController.handshakeEventReceived("Connected to ${serverCertificate.certificate.name}")
            }
        }
        state++
    }
}
