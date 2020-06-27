package com.example.attendance.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

data class ClasslistInstance(
    val parent: ClasslistGroup? = null,
    val id: String = "",
    val studentState: MutableMap<String, String> = mutableMapOf(),
    val created: Timestamp = Timestamp.now(),
    val modified: Timestamp = Timestamp.now()
) {
    fun setStudentState(student: Student, state: Tag) {
        studentState[student.id] = state.id
        Firebase.firestore.collection("attendance")
            .document(parent!!.id)
            .collection("lists")
            .document(id)
            .update("studentState", studentState, "modified", Timestamp.now())
    }
}
