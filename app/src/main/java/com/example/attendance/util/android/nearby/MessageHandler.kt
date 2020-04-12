package com.example.attendance.util.android.nearby

import com.example.attendance.util.android.nearby.protocols.Handshake
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate


class MessageHandler(connection: ConnectionsClient, endpointId: String, advertising: Boolean) :
    PayloadCallback() {
    val handshake = Handshake(connection, endpointId, advertising)


    override fun onPayloadReceived(endpointId: String, payload: Payload) {
        println("Received payload ${String(payload.asBytes()!!)}")
        if (payload.type == Payload.Type.BYTES) handshake.next(String(payload.asBytes()!!))
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
    }
}
