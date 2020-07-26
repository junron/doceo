package com.example.attendance.viewmodels

import androidx.lifecycle.ViewModel
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.SafeLiveData
import com.example.attendance.util.auth.currentUserEmail
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AssignmentsViewModel : ViewModel() {
    val assignments: SafeLiveData<List<Assignment>> = SafeLiveData(listOf())
    val submissions: SafeLiveData<List<Submission>> = SafeLiveData(listOf())
    lateinit var user: String
    var currentAssignmentId: String? = null
    var currentSubmissionId: String? = null

    fun getAssignment() = assignments.value.find {
        it.id == currentAssignmentId
    }

    fun getSubmission() = submissions.value.find {
        it.id == currentSubmissionId
    }

    fun updateComment(comment: String) {
        Firebase.firestore.collection("submissions")
            .document(currentSubmissionId!!)
            .set(
                mapOf(
                    "comment" to comment
                ),
                SetOptions.merge()
            )
    }

    fun deleteAssignment() {
        Firebase.firestore.collection("assignments")
            .document(currentAssignmentId!!)
            .set(
                mapOf(
                    "deleted" to true
                ),
                SetOptions.merge()
            )
    }

    fun restoreAssignment(id: String) {
        Firebase.firestore.collection("assignments")
            .document(id)
            .set(
                mapOf(
                    "deleted" to false
                ),
                SetOptions.merge()
            )
    }

    fun createAssignment(assignment: Assignment) {
        Firebase.firestore.collection("assignments")
            .document(assignment.id)
            .set(assignment)
        assignments.value += assignment
    }

    init {
        setup()
        setListeners()
    }

    fun setup() {
        user = currentUserEmail()
        assignments.value = emptyList()
        submissions.value = emptyList()
        // Owned assignments
        Firebase.firestore
            .collection("assignments")
            .whereEqualTo("owner", user)
            .get()
            .addOnSuccessListener {
                val assignments = it.toObjects(Assignment::class.java)
                this.assignments.value += assignments
                this.assignments.value = this.assignments.value.distinctBy { it.id }
                    .filter { !it.deleted }
                val assignmentIds = assignments.map { it.id }
                reloadSubmissions(assignmentIds)
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
                    .filter { !it.deleted }
            }
        // Own submissions
        Firebase.firestore
            .collection("submissions")
            .whereEqualTo("owner", user)
            .get()
            .addOnSuccessListener {
                val objects = it.toObjects(Submission::class.java)
                submissions.value += objects
                this.submissions.value = this.submissions.value.distinctBy { it.id }
                    .filter { !it.deleted }
            }
            .addOnFailureListener {
                println("Failure: $it")
            }
    }

    private fun reloadSubmissions(assignmentIds: List<String>) {
        if (assignmentIds.isEmpty()) return
        // Student submissions
        Firebase.firestore
            .collection("submissions")
            .whereIn("assignmentId", assignmentIds)
            .get()
            .addOnSuccessListener { submissionSnapshot ->
                val submissions = submissionSnapshot.toObjects(Submission::class.java)
                this.submissions.value += submissions
                this.submissions.value = this.submissions.value.distinctBy { it.id }
                    .filter { !it.deleted }
            }
    }

    private fun setListeners() {
        Firebase.firestore
            .collection("assignments")
            .whereEqualTo("owner", user)
            .addSnapshotListener { value, _ ->
                value ?: return@addSnapshotListener
                val assignments = value.toObjects(Assignment::class.java)
                this.assignments.value = ((this.assignments.value - this.assignments.value.filter {
                    it.owner == user
                }) + assignments)
                    .distinctBy { it.id }
                    .filter { !it.deleted }

                val assignmentIds = assignments.map { it.id }
                reloadSubmissions(assignmentIds)
                if (assignmentIds.isEmpty()) return@addSnapshotListener
                // Student submissions
                Firebase.firestore
                    .collection("submissions")
                    .whereIn("assignmentId", assignmentIds)
                    .addSnapshotListener submissionsSnapshot@{ submissionSnapshot, _ ->
                        submissionSnapshot ?: return@submissionsSnapshot
                        val submissions = submissionSnapshot.toObjects(Submission::class.java)
                        this.submissions.value =
                            ((this.submissions.value - this.submissions.value.filter {
                                it.assignmentId in assignmentIds
                            }) + submissions)
                                .distinctBy { it.id }
                                .filter { !it.deleted }
                    }
            }
        Firebase.firestore
            .collection("assignments")
            .whereArrayContains("students", user)
            .addSnapshotListener { value, _ ->
                value ?: return@addSnapshotListener
                val assignments = value.toObjects(Assignment::class.java)
                this.assignments.value = ((this.assignments.value - this.assignments.value.filter {
                    user in it.students
                }) + assignments)
                    .distinctBy { it.id }
                    .filter { !it.deleted }
            }
        Firebase.firestore
            .collection("submissions")
            .whereEqualTo("owner", user)
            .addSnapshotListener { value, _ ->
                value ?: return@addSnapshotListener
                val objects = value.toObjects(Submission::class.java)
                this.submissions.value = ((this.submissions.value - this.submissions.value.filter {
                    it.owner == user
                }) + objects)
                    .distinctBy { it.id }
                    .filter { !it.deleted }
            }
    }

}
