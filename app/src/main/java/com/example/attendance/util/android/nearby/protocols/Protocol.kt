package com.example.attendance.util.android.nearby.protocols

import com.example.attendance.util.android.nearby.NearbyMessage
import com.example.attendance.util.android.nearby.connection
import com.google.android.gms.nearby.connection.ConnectionsClient

abstract class Protocol(val connection: ConnectionsClient, val endpointId: String) {
    var state = 0
    val sendPayload = connection.connection(endpointId)
    abstract fun next(message: NearbyMessage)
}
