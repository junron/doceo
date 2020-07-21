package com.example.attendance.viewmodels

import androidx.lifecycle.ViewModel
import com.example.attendance.models.ClasslistGroup
import com.example.attendance.util.android.SafeLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class ClasslistGroupViewModel : ViewModel() {
    companion object {
        private var cache = mutableListOf<ClasslistGroup>()
    }

    val classlistGroups = SafeLiveData(cache.toList())
    private var classlistGroupsTemp = listOf<ClasslistGroup>()
    private var loadStatus = 0
    private val processQuery = { documents: List<DocumentSnapshot> ->
        documents.mapNotNull {
            try {
                it.toObject(ClasslistGroup::class.java)?.copy(id = it.id)
            } catch (e: RuntimeException) {
                null
            }
        }
    }

    init {
        setup()
    }

    private fun setup() {
        val user =
            FirebaseAuth.getInstance().currentUser ?: return run {
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    if (auth.currentUser != null) setup()
                }
            }
        val query = user.email!!
        Firebase.firestore.collection("attendance")
            .whereEqualTo("owner", query)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    classlistGroupsTemp =
                        classlistGroupsTemp + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        classlistGroups.value = classlistGroupsTemp
                        cache = classlistGroupsTemp.toMutableList()
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                println("Updated")
                getAttendance {
                    classlistGroupsTemp = it
                    classlistGroups.value = classlistGroupsTemp
                    cache = classlistGroupsTemp.toMutableList()
                }
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("editors", query)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    classlistGroupsTemp =
                        classlistGroupsTemp + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        classlistGroups.value = classlistGroupsTemp
                        cache = classlistGroupsTemp.toMutableList()
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                getAttendance {
                    classlistGroupsTemp = it
                    classlistGroups.value = classlistGroupsTemp
                    cache = classlistGroupsTemp.toMutableList()
                }
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("viewers", query)
            .addSnapshotListener { snapshot, _ ->
                snapshot ?: return@addSnapshotListener
                if (loadStatus != 3) {
                    classlistGroupsTemp =
                        classlistGroupsTemp + processQuery(snapshot.documents)
                    if (loadStatus == 2) {
                        classlistGroups.value = classlistGroupsTemp
                        cache = classlistGroupsTemp.toMutableList()
                    }
                    loadStatus++
                    return@addSnapshotListener
                }
                getAttendance {
                    classlistGroupsTemp = it
                    classlistGroups.value = classlistGroupsTemp
                    cache = classlistGroupsTemp.toMutableList()
                }
            }
    }

    private fun getAttendance(callback: (List<ClasslistGroup>) -> Unit) {
        val user =
            FirebaseAuth.getInstance().currentUser ?: return run {
                FirebaseAuth.getInstance().addAuthStateListener { auth ->
                    if (auth.currentUser != null) setup()
                }
            }
        val query = user.email!!
        var queries = 0
        val data = mutableListOf<ClasslistGroup>()
        Firebase.firestore.collection("attendance")
            .whereEqualTo("owner", query)
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }

        Firebase.firestore.collection("attendance")
            .whereArrayContains("editors", query)
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }
        Firebase.firestore.collection("attendance")
            .whereArrayContains("viewers", query)
            .get().addOnSuccessListener {
                data += processQuery(it.documents)
                queries++
                if (queries == 3) callback(data)
            }
    }
}
