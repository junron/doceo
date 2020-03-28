package com.example.attendance.models

import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.ocpsoft.prettytime.PrettyTime

data class Attendance(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val editors: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
    val created: Timestamp = Timestamp.now(),
    val modified: Timestamp = Timestamp.now(),
    val deleted: Boolean = false
) {
    fun getAccessLevel(uid: String) = when (uid) {
        owner -> AccessLevel.OWNER
        in editors -> AccessLevel.EDITOR
        in viewers -> AccessLevel.VIEWER
        else -> AccessLevel.NONE
    }

    fun getCreatedTime() = PrettyTime().format(created.toDate()) as String
    fun getModifiedTime() = PrettyTime().format(modified.toDate()) as String

    fun delete() {
        Firebase.firestore.collection("attendance")
            .document(id)
            .update("deleted", true, "modified", Timestamp.now())
    }

    fun restore() {
        Firebase.firestore.collection("attendance")
            .document(id)
            .update("deleted", false, "modified", Timestamp.now())
    }

    fun rename(name: String) {
        Firebase.firestore.collection("attendance")
            .document(id)
            .update("name", name, "modified", Timestamp.now())
    }
}

object AttendanceLoader {
    var attendance = emptyList<Attendance>()
        private set

    private val listeners = mutableListOf<(List<Attendance>) -> Unit>()

    fun addListener(callback: (List<Attendance>) -> Unit) {
        listeners += callback
    }

    private val processQuery = { documents: List<DocumentSnapshot> ->
        documents.mapNotNull {
            it.toObject(Attendance::class.java)?.copy(id = it.id)
        }
    }


    fun setup() {
        println("Attendance setup")
        val user =
            FirebaseAuth.getInstance().currentUser ?: return run {
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    if (auth.currentUser != null) setup()
                }
            }
        var queries = 0
        val data = mutableListOf<Attendance>()
        Firebase.firestore.collection("attendance")
            .whereEqualTo("owner", user.uid)
            .apply {
                addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    val changedData = processQuery(snapshot.documents)
                    attendance = changedData
                    listeners.forEach {
                        it(changedData)
                    }
                }
                get().addOnSuccessListener {
                    data += processQuery(it.documents)
                    queries++
                    if (queries == 3) attendance = data
                }
            }
        Firebase.firestore.collection("attendance")
            .whereArrayContains("editors", user.uid)
            .apply {
                addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    val changedData = processQuery(snapshot.documents)
                    attendance = changedData
                    listeners.forEach {
                        it(changedData)
                    }
                }
                get().addOnSuccessListener {
                    data += processQuery(it.documents)
                    queries++
                    if (queries == 3) attendance = data
                }
            }
        Firebase.firestore.collection("attendance")
            .whereArrayContains("viewers", user.uid)
            .apply {
                addSnapshotListener { snapshot, _ ->
                    snapshot ?: return@addSnapshotListener
                    val changedData = processQuery(snapshot.documents)
                    attendance = changedData
                    listeners.forEach {
                        it(changedData)
                    }
                }
                get().addOnSuccessListener {
                    data += processQuery(it.documents)
                    queries++
                    if (queries == 3) attendance = data
                }
            }
    }
}
