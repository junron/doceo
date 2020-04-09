package com.example.attendance.util.android.nearby.protocols

import android.content.Context
import com.example.attendance.controllers.ClasslistController
import com.example.attendance.controllers.NearbyController
import com.example.attendance.util.android.nearby.AndroidNearby
import com.example.attendance.util.android.nearby.toPayload
import com.example.attendance.util.auth.Crypto
import com.example.attendance.util.auth.Crypto.Companion.loadKeyBytes
import com.example.attendance.util.auth.User
import com.example.attendance.util.auth.UserLoader.getCertificate
import com.example.attendance.util.auth.models.SignedCertificate
import com.google.android.gms.nearby.connection.ConnectionsClient
import kotlinx.serialization.SerializationException
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json

@UnstableDefault
class Handshake(
    connection: ConnectionsClient,
    endpointId: String,
    private val advertising: Boolean
) :
    Protocol(connection, endpointId) {
    /*
    Steps:
    0: Server: sends certificate to client; Client: Verify certificate and send challenge to server
    1: Server: Receive challenge, generate response; Client: verify server response, send certificates
    2: Server: Verify certificates and send challenge; Client: Generate response to challenge
    3: Server: Verify response, sends OK; Client: Receives OK, closes connection
     */
    private lateinit var serverCertificate: SignedCertificate
    private lateinit var serverChallenge: String

    companion object {
        val clientCertificateString =
            Json.stringify(SignedCertificate.serializer(), getCertificate())
        val clientCrypto = Crypto()
        fun init(context: Context) {
            clientCrypto.loadKeysFromFile(context)
        }
    }

    private fun setRemoteCertificate(certificate: String): Boolean {
        return try {
            serverCertificate = Json.parse(SignedCertificate.serializer(), certificate)
            true
        } catch (error: SerializationException) {
            println("Invalid json: $certificate")
            false
        }
    }

    private fun generateChallenge(): String {
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

    private fun verifyRemoteResponse(signature: String) = try {
        Crypto.verify(
            serverChallenge,
            signature,
            serverCertificate.certificate.publicKey.loadKeyBytes()
        )
    } catch (e: Exception) {
        println("Verification exception: $e")
        false
    }

    override fun next(message: String) {
        when (state) {
            0 -> {
                if (advertising) sendPayload(clientCertificateString.toPayload())
                else {
                    val result = setRemoteCertificate(message)
                    if (!result) {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    //  Send challenge to remote
                    sendPayload(generateChallenge().toPayload())
                }
            }
            1 -> {
                if (advertising) {
                    val response = respondToChallenge(message) ?: kotlin.run {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    sendPayload(response.toPayload())
                } else {
                    if (!verifyRemoteResponse(message)) {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    val cert = serverCertificate.certificate
                    val remoteUser = User(cert.name, cert.id, cert.metadata)
                    if (!remoteUser.isMentorRep) {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    // Connected to Mentor Rep
                    println("Connected to ${serverCertificate.certificate.name}")
                    NearbyController.onConnected(remoteUser)
                    sendPayload(clientCertificateString.toPayload())
                }
            }
            2 -> {
                if (advertising) {
                    val result = setRemoteCertificate(message)
                    if (!result) {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    //  Send challenge to peer
                    sendPayload(generateChallenge().toPayload())
                } else {
                    val response = respondToChallenge(message) ?: kotlin.run {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    sendPayload(response.toPayload())
                }
            }
            3 -> {
                if (advertising) {
                    if (!verifyRemoteResponse(message)) {
                        connection.disconnectFromEndpoint(endpointId)
                        return
                    }
                    val cert = serverCertificate.certificate
                    val remoteUser = User(cert.name, cert.id, cert.metadata)
                    println("Connected to ${serverCertificate.certificate.name}")
                    val result = ClasslistController.onNearbyCompleted(remoteUser)
                    if (result) {
                        sendPayload("RECORD_OK".toPayload())
                    } else {
                        sendPayload("CONN_OK".toPayload())
                    }
                } else {
                    connection.disconnectFromEndpoint(endpointId)
                    if (message == "RECORD_OK") {
                        println("Connection established!")
                        NearbyController.onHandshakeComplete()
                        AndroidNearby.stopDiscovery()
                    } else if (message == "CONN_OK") {
                        println("Connection succeeded, but attendance not recorded")
                    }
                }
            }
        }
        state++
    }
}
