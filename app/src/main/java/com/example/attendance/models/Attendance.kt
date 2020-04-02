package com.example.attendance.models

import com.example.attendance.util.AppendOnlyStorage
import com.example.attendance.util.toDate
import com.example.attendance.util.toStringValue
import com.example.attendance.util.uuid
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.serialization.Serializable
import org.ocpsoft.prettytime.PrettyTime
import java.util.*

data class Attendance(
    val id: String = "",
    val name: String = "",
    val owner: String = "",
    val editors: List<String> = emptyList(),
    val viewers: List<String> = emptyList(),
    val created: Timestamp = Timestamp.now(),
    val modified: Timestamp = Timestamp.now(),
    val constraints: String = "",
    val tags: Map<String, Int> = emptyMap(),
    val deleted: Boolean = false
) {
    companion object {
        fun newAttendance(name: String, tags: List<Tag>, constraints: String) {
            Firebase.firestore.collection("attendance")
                .document(uuid())
                .set(
                    mapOf(
                        "name" to name,
                        "owner" to FirebaseAuth.getInstance().uid,
                        "editors" to emptyList<String>(),
                        "viewers" to emptyList<String>(),
                        "tags" to tags.map { it.name to it.color }.toMap(),
                        "constraints" to constraints,
                        "created" to Timestamp.now(),
                        "modified" to Timestamp.now(),
                        "deleted" to false
                    )
                )
        }
    }

    @ExperimentalStdlibApi
    val permissions = buildList {
        add(Permission(owner, AccessLevel.OWNER, id))
        addAll(editors.sorted().map {
            Permission(it, AccessLevel.EDITOR, id)
        })
        addAll(viewers.sorted().map {
            Permission(it, AccessLevel.VIEWER, id)
        })
    }

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

    fun changePermissions(uid: String, accessLevel: AccessLevel) {
        val updateFields = mutableMapOf<String, Any>("modified" to Timestamp.now())
        val currentAcl = getAccessLevel(uid)
        if (accessLevel == currentAcl) return
        when (currentAcl) {
            AccessLevel.EDITOR -> updateFields["editors"] = editors - uid
            AccessLevel.VIEWER -> updateFields["viewers"] = viewers - uid
            AccessLevel.OWNER -> return
            AccessLevel.NONE -> return
        }
        when (accessLevel) {
            // Cannot change to owner
            AccessLevel.OWNER -> return
            AccessLevel.EDITOR -> updateFields["editors"] = editors + uid
            AccessLevel.VIEWER -> updateFields["viewers"] = viewers + uid
            AccessLevel.NONE -> Unit
        }
        Firebase.firestore.collection("attendance")
            .document(id)
            .update(updateFields)
    }

    fun share(students: List<Student>, edit: Boolean) {
        val new = (if (edit) editors else viewers) + students.map { it.id }
        Firebase.firestore.collection("attendance")
            .document(id)
            .update(if (edit) "editors" else "viewers", new, "modified", Timestamp.now())
    }

    fun opened() {
        AttendanceLoader.history += AttendanceHistory(id, Date().toStringValue())
    }

    fun lastOpenedDate(): String {
        val latest = AttendanceLoader.history.filter { it.id == id }
            .maxBy { it.time.toDate().time } ?: return "Never opened"
        return "Opened ${PrettyTime().format(latest.time.toDate())}"
    }
}

@Serializable
data class AttendanceHistory(val id: String, val time: String)

object AttendanceLoader {
    val history = AppendOnlyStorage("attendance-history", AttendanceHistory.serializer())
    var attendance = emptyList<Attendance>()
        private set

    private val listeners = mutableListOf<(List<Attendance>) -> Unit>()

    fun addListener(callback: (List<Attendance>) -> Unit) {
        listeners += callback
    }

    fun removeListener(callback: (List<Attendance>) -> Unit) {
        listeners.removeAll {
            it == callback
        }
    }

    private val processQuery = { documents: List<DocumentSnapshot> ->
        documents.mapNotNull {
            it.toObject(Attendance::class.java)?.copy(id = it.id)
        }
    }


    fun setup() {
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
                    attendance = attendance.filter { user.uid != it.owner } + changedData
                    listeners.forEach {
                        it(attendance)
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
                    attendance = attendance.filter { user.uid !in it.editors } + changedData
                    listeners.forEach {
                        it(attendance)
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
                    attendance = attendance.filter { user.uid !in it.viewers } + changedData
                    listeners.forEach {
                        it(attendance)
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

