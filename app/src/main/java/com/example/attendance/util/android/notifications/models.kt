package com.example.attendance.util.android.notifications

import kotlinx.serialization.Serializable

@Serializable
data class AddTokenRequest(val token: String, val auth: String)

@Serializable
data class SendNotificationRequest(
    val targetId: String,
    val data: Map<String, String>,
    val auth: String
)
