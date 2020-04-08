package com.example.attendance.util.android.nearby

import com.google.android.gms.nearby.connection.ConnectionsClient
import com.google.android.gms.nearby.connection.Payload

fun ConnectionsClient.connection(endpointId: String) = { payload: Payload ->
    this.sendPayload(endpointId, payload)
}

fun String.toPayload() = Payload.fromBytes(toByteArray())
