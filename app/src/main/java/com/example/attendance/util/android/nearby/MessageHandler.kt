package com.example.attendance.util.android.nearby

import com.example.attendance.util.android.nearby.protocols.Handshake
import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload
import com.google.android.gms.nearby.connection.PayloadCallback
import com.google.android.gms.nearby.connection.PayloadTransferUpdate


class MessageHandler(connection: ConnectionsClient, endpointId: String, advertising: Boolean) :
    PayloadCallback() {
    private val handshake = Handshake(connection, endpointId, advertising)

    init {
        if (advertising)
            handshake.next("")
    }

    override fun onPayloadReceived(endpointId: String, payload: Payload) {
        if (payload.type == Payload.Type.BYTES) handshake.next(String(payload.asBytes()!!))
    }

    override fun onPayloadTransferUpdate(endpointId: String, update: PayloadTransferUpdate) {
    }
}
