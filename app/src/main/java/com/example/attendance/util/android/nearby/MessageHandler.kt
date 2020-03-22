package com.example.attendance.util.android.nearby

import com.example.attendance.util.android.nearby.protocols.Handshake
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

enum class NearbyStage { HANDSHAKE }

@Serializable
data class NearbyMessage(val stage: NearbyStage, val data: String) {
    fun toPayload() = Payload.fromBytes(
        Json.stringify(serializer(), this).toByteArray()
    )
}

class MessageHandler(connection: ConnectionsClient, endpointId: String) :
    PayloadCallback() {
    private val handshake = Handshake(connection, endpointId)
    val sendPayload = connection.connection(endpointId)
    override fun onPayloadReceived(endpointId: String, payload: Payload) {
        when (payload.type) {
            Payload.Type.BYTES -> try {
                val message = Json.parse(NearbyMessage.serializer(), String(payload.asBytes()!!))
                when (message.stage) {
                    NearbyStage.HANDSHAKE -> handshake.next(message)
                }
            } catch (e: SerializationException) {

            }
        }
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
        println("Payload Update: $endpointId $update")
    }
}
