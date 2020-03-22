package com.example.attendance.util.android.nearby

import android.content.Context
import com.example.attendance.util.android.nearby.protocols.Handshake
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes.*
import com.google.firebase.auth.FirebaseAuth


val state = mutableMapOf<String, MessageHandler>()

object AndroidNearby {
    private lateinit var context: Context
    private val connectionCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            when (result.status.statusCode) {
                STATUS_OK -> {
                    println("Connection succeeded!")
                    val handler = state[endpointId] ?: return
                    handler.sendPayload(
                        NearbyMessage(
                            NearbyStage.HANDSHAKE, Handshake.clientCertificateString
                        ).toPayload()
                    )
                }
                STATUS_CONNECTION_REJECTED -> println("Connection refused!")
                STATUS_ERROR -> println("Connection broke!")
            }
        }

        override fun onDisconnected(endpointId: String) {
            println("$endpointId died.")
        }

        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            println(info.endpointName)
            val handler = MessageHandler(Nearby.getConnectionsClient(context), endpointId)
            Nearby.getConnectionsClient(context)
                .acceptConnection(
                    endpointId,
                    handler
                )
        }

    }

    private val discoveryCallback = object : EndpointDiscoveryCallback() {
        override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
            Nearby.getConnectionsClient(context)
                .requestConnection("hello, world", endpointId, connectionCallback)
                .addOnSuccessListener {
                    println("Connection Succeeded!")
                }
                .addOnFailureListener {
                    println("Connection fail: $it")
                }
        }

        override fun onEndpointLost(endpointId: String) {
            println("$endpointId died.")
        }

    }

    fun init(context: Context) {
        this.context = context
    }

    fun startAdvertising() {
        val auth = FirebaseAuth.getInstance()
        val advertisingOptions: AdvertisingOptions =
            AdvertisingOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(context)
            .startAdvertising(
                auth.currentUser?.uid.toString(),
                "com.example.attendance",
                connectionCallback,
                advertisingOptions
            )
            .addOnSuccessListener {
                println("Started advertising")
            }
            .addOnFailureListener {
                println("Failed: $it")
            }
    }

    fun startDiscovery() {
        val discoveryOptions =
            DiscoveryOptions.Builder().setStrategy(Strategy.P2P_CLUSTER).build()
        Nearby.getConnectionsClient(context)
            .startDiscovery("com.example.attendance", discoveryCallback, discoveryOptions)
            .addOnSuccessListener { println("Discovery started") }
            .addOnFailureListener {
                println("Discovery Failed: $it")
            }
    }

    fun stop() {
        println("Stopping everything")
        Nearby.getConnectionsClient(context)
            .apply {
                stopDiscovery()
                stopAdvertising()
                stopAllEndpoints()
            }
    }

}
