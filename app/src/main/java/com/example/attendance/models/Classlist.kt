package com.example.attendance.models

import com.example.attendance.util.toStringValue
import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.*

data class ClasslistInstance(
    val parent: Attendance? = null,
    val id: String = "",
    val events: MutableList<String> = mutableListOf(),
    val created: Timestamp = Timestamp.now(),
    val modified: Timestamp = Timestamp.now()
) {
    fun addEvent(event: ClasslistEvent) {
        events += Json.stringify(ClasslistEvent.serializer(), event)
    }
}

@Serializable
sealed class ClasslistEvent {
    @Serializable
    data class Opened(val uid: String)

    @Serializable
    data class Closed(val uid: String)

    @Serializable
    data class SetState(val targetId: String, val state: Tag, val timestamp: String) {
        constructor(targetId: String, state: Tag) : this(
            targetId, state, Date().toStringValue()
        )
    }
}
