package com.example.attendance.models

import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon
import android.os.Build
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.AppendOnlyStorage
import com.example.attendance.util.android.notifications.NotificationServer
import com.example.attendance.util.android.notifications.SendNotificationRequest
import com.example.attendance.util.toDate
import com.example.attendance.util.toStringValue
import com.example.attendance.util.uuid
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
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
    val tags: List<String> = emptyList(),
    val deleted: Boolean = false
) {
    var classlists: List<ClasslistInstance> = listOf()
        private set
    private val listeners = mutableListOf<(List<ClasslistInstance>) -> Unit>()
    val students = Students.filterStudents(constraints.split(" "))

    companion object {
        fun newAttendance(name: String, tags: List<Tag>, constraints: String) {
            val id = uuid()
            Firebase.firestore.collection("attendance")
                .document(id)
                .set(
                    mapOf(
                        "name" to name,
                        "owner" to FirebaseAuth.getInstance().uid,
                        "editors" to emptyList<String>(),
                        "viewers" to emptyList<String>(),
                        "tags" to tags.map { Json.stringify(Tag.serializer(), it) },
                        "constraints" to constraints,
                        "created" to Timestamp.now(),
                        "modified" to Timestamp.now(),
                        "deleted" to false
                    )
                )
            Firebase.firestore.collection("attendance")
                .document(id)
                .collection("lists")
                .document(uuid())
                .set(
                    mapOf(
                        "studentState" to emptyMap<String, String>(),
                        "created" to Timestamp.now(),
                        "modified" to Timestamp.now()
                    )
                )
        }
    }

    init {
        if (id != "") loadClasslists()
    }

    fun loadClasslists() {
        Firebase.firestore.collection("attendance")
            .document(id)
            .collection("lists")
            .apply {
                get().addOnSuccessListener { snapshotList ->
                    this@Attendance.classlists = snapshotList.documents.mapNotNull { snapshot ->
                        try {
                            snapshot.toObject(ClasslistInstance::class.java)
                                ?.copy(parent = this@Attendance, id = snapshot.id)
                        } catch (e: Exception) {
                            println(e)
                            null
                        }
                    }.sortedBy { it.created }
                }
                    .addOnFailureListener {
                        loadClasslists()
                    }
                addSnapshotListener { snapshotList, _ ->
                    snapshotList ?: return@addSnapshotListener
                    this@Attendance.classlists = snapshotList.documents.mapNotNull { snapshot ->
                        try {
                            snapshot.toObject(ClasslistInstance::class.java)
                                ?.copy(parent = this@Attendance, id = snapshot.id)
                        } catch (e: Exception) {
                            println(e)
                            null
                        }
                    }.sortedBy { it.created }
                    listeners.forEach { it(this@Attendance.classlists) }
                }
            }
    }

    fun addListener(callback: (List<ClasslistInstance>) -> Unit) {
        listeners += callback
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
        GlobalScope.launch {
            NotificationServer.sendNotification(
                SendNotificationRequest(
                    students.map { it.id },
                    mapOf(
                        "type" to "ITEM_SHARED",
                        "attendanceId" to id,
                        "attendanceName" to name,
                        "owner" to Students.getStudentById(owner)!!.name
                    ),
                    ""
                )
            )
        }
    }

    fun opened() {
        AttendanceLoader.history += AttendanceHistory(id, Date().toStringValue())
    }


    fun lastOpenedDate(): String {
        val latest = getLastAccess() ?: return "Never opened"
        return "Opened ${PrettyTime().format(latest)}"
    }

    fun getParsedTags() = tags.mapNotNull {
        try {
            Json.parse(Tag.serializer(), it)
        } catch (e: SerializationException) {
            null
        }
    }

    fun newClasslist(): String {
        val classlistId = uuid()
        Firebase.firestore.collection("attendance")
            .document(id)
            .collection("lists")
            .document(classlistId)
            .set(
                mapOf(
                    "studentState" to emptyMap<String, String>(),
                    "created" to Timestamp.now(),
                    "modified" to Timestamp.now()
                )
            )
        return classlistId
    }

    fun isInitialized() = classlists.isNotEmpty()

    fun addToHomeScreen() {
        val shortcutIntent =
            Intent(MainActivity.activity.applicationContext, MainActivity::class.java).apply {
                putExtra("attendance_id", id)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                action = Intent.ACTION_CREATE_SHORTCUT
            }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val shortcut = ShortcutInfo.Builder(MainActivity.activity, id)
                .setShortLabel(name)
                .setLongLabel("Open $name")
                .setIcon(
                    Icon.createWithResource(
                        MainActivity.activity,
                        R.drawable.app_icon
                    )
                )
                .setIntent(shortcutIntent)
                .build()
            val shortcutManager =
                MainActivity.activity.getSystemService(ShortcutManager::class.java)
            shortcutManager.requestPinShortcut(shortcut, null)
        } else {
            val addIntent = Intent()
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, name)
            addIntent.putExtra(
                Intent.EXTRA_SHORTCUT_ICON_RESOURCE,
                Intent.ShortcutIconResource.fromContext(
                    MainActivity.activity,
                    R.drawable.app_icon
                )
            )
            addIntent.action = "com.android.launcher.action.INSTALL_SHORTCUT"
            addIntent.putExtra("duplicate", false)
            MainActivity.activity.sendBroadcast(addIntent)
        }
    }

    fun getLastAccess() = AttendanceLoader.history.filter { it.id == id }
        .maxBy { it.time.toDate().time }?.time?.toDate()
}

@Serializable
data class AttendanceHistory(val id: String, val time: String)

object AttendanceLoader {
    val history = AppendOnlyStorage("attendance-history", AttendanceHistory.serializer())
    var attendance = emptyList<Attendance>()
        private set
    private var loadStatus = 0

    private val listeners = mutableListOf<(List<Attendance>) -> Unit>()

    fun addListener(callback: (List<Attendance>) -> Unit) {
        listeners += callback
    }

    private val processQuery = { documents: List<DocumentSnapshot> ->
        documents.mapNotNull {
            try {
                it.toObject(Attendance::class.java)?.copy(id = it.id)
            } catch (e: RuntimeException) {
                null
            }
        }
    }


    fun setup() {
        val user =
            FirebaseAuth.getInstance().currentUser ?: return run {
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    if (auth.currentUser != null) setup()
                }
            }
        Firebase.firestore.collection("attendance")
            .whereEqualTo("owner", user.uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    attendance = attendance + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        listeners.forEach {
                            it(attendance)
                        }
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                getAttendance {
                    attendance = it
                    listeners.forEach {
                        it(attendance)
                    }
                }
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("editors", user.uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    attendance = attendance + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        listeners.forEach {
                            it(attendance)
                        }
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                getAttendance {
                    attendance = it
                    listeners.forEach {
                        it(attendance)
                    }
                }
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("viewers", user.uid)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    attendance = attendance + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        listeners.forEach {
                            it(attendance)
                        }
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                getAttendance {
                    attendance = it
                    listeners.forEach {
                        it(attendance)
                    }
                }
            }
    }

    private fun getAttendance(callback: (List<Attendance>) -> Unit) {
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
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("editors", user.uid)
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }
        Firebase.firestore.collection("attendance")
            .whereArrayContains("viewers", user.uid)
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }
    }
}

