package com.example.attendance.viewmodels

import androidx.lifecycle.ViewModel
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.SafeLiveData
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AssignmentsViewModel : ViewModel() {
    val assignments: SafeLiveData<List<Assignment>> = SafeLiveData(listOf())
    val submissions: SafeLiveData<List<Submission>> = SafeLiveData(listOf())
    var currentAssignmentId: String? = null
    var currentSubmissionId: String? = null

    fun getAssignment() = assignments.value.find {
        it.id == currentAssignmentId
    }

    fun getSubmission() = submissions.value.find {
        it.id == currentSubmissionId
    }

    fun deleteAssignment(callback: (success: Boolean) -> Unit) {
        Firebase.firestore.collection("assignments")
            .document(currentAssignmentId!!)
            .delete()
            .addOnSuccessListener {
                callback(true)
            }
            .addOnFailureListener {
                callback(false)
            }
    }

    init {
        setup()
    }

    fun setup() {
        val user = FirebaseAuth.getInstance().currentUser?.email!!
        assignments.value = emptyList()
        submissions.value = emptyList()
        // Owned assignments
        Firebase.firestore
            .collection("assignments")
            .whereEqualTo("owner", user)
            .get()
            .addOnSuccessListener {
                val assignments = it.toObjects(Assignment::class.java)
                val assignmentIds = assignments.map { it.id }
                // Own submissions
                Firebase.firestore
                    .collection("submissions")
                    .whereIn("assignmentId", assignmentIds)
                    .get()
                    .addOnSuccessListener { submissionSnapshot ->
                        val submissions = submissionSnapshot.toObjects(Submission::class.java)
                        this.submissions.value += submissions
                        this.submissions.value = this.submissions.value.distinctBy { it.id }
                    }
                this.assignments.value += assignments
                this.assignments.value = this.assignments.value.distinctBy { it.id }
            }
        // Student assignments
        Firebase.firestore
            .collection("assignments")
            .whereArrayContains("students", user)
            .get()
            .addOnSuccessListener {
                val objects = it.toObjects(Assignment::class.java)
                assignments.value += objects
                this.assignments.value = this.assignments.value.distinctBy { it.id }
            }
        // Own submissions
        Firebase.firestore
            .collection("submissions")
            .whereArrayContains("owner", user)
            .get()
            .addOnSuccessListener {
                val objects = it.toObjects(Submission::class.java)
                submissions.value += objects
                println(submissions)
                this.submissions.value = this.submissions.value.distinctBy { it.id }
            }
            .addOnFailureListener {
                println("Failure: $it")
            }
    }

}
