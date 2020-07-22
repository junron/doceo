package com.example.attendance.viewmodels

import androidx.lifecycle.ViewModel
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.SafeLiveData
import com.google.android.gms.tasks.Task
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult

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

    fun deleteAssignment(callback: (Task<HttpsCallableResult>) -> Unit) {
        val query = mapOf("id" to currentAssignmentId)
        FirebaseFunctions.getInstance().getHttpsCallable("deleteAssignment")
            .call(query)
            .continueWith { task: Task<HttpsCallableResult> ->
                callback(task)
            }
    }

}
