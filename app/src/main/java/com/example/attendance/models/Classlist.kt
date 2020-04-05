package com.example.attendance.models

import com.example.attendance.util.toStringValue
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
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
        val eventString = Json.stringify(ClasslistEvent.serializer(), event)
        events += eventString
        Firebase.firestore.collection("attendance")
            .document(parent!!.id)
            .collection("lists")
            .document(id)
            .update("events", FieldValue.arrayUnion(eventString), "modified", Timestamp.now())
    }

    fun getParsedEvents() = events.mapNotNull {
        try {
            Json.parse(ClasslistEvent.serializer(), it)
        } catch (e: SerializationException) {
            null
        }
    }
}

@Serializable
sealed class ClasslistEvent {
    @Serializable
    data class Opened(val uid: String) : ClasslistEvent()

    @Serializable
    data class Closed(val uid: String) : ClasslistEvent()

    @Serializable
    data class StateChanged(val targetId: String, val state: String, val timestamp: String) :
        ClasslistEvent() {
        constructor(targetId: String, state: String) : this(
            targetId, state, Date().toStringValue()
        )
    }
}
