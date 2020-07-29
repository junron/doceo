package com.example.attendance.fragments.snapmit.submit

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.viewModelScope
import androidx.navigation.fragment.NavHostFragment
import com.example.attendance.R
import com.example.attendance.models.snapmit.Submission
import com.example.attendance.util.android.CloudStorage
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.SafeLiveData
import com.example.attendance.util.auth.currentUserEmail
import com.example.attendance.util.pmap
import com.example.attendance.util.uuid
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.example.attendance.viewmodels.SubmitViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.fragment_loading.*
import kotlinx.android.synthetic.main.fragment_loading.view.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.*

class LoadingFragment : Fragment() {
    private val submitViewModel: SubmitViewModel by activityViewModels()
    private val assignmentsViewModel: AssignmentsViewModel by activityViewModels()
    private lateinit var coroutineJob: Job

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_loading, container, false)
        view.loading_text.text = "Uploading images"
        coroutineJob = submitViewModel.viewModelScope.launch(Dispatchers.IO) {
            val urls = submitViewModel.imagesData.value.pmap {
                CloudStorage.addObject(it)
            }
            setLoadingText("Submitting", view.loading_text)
            val submission = Submission(
                submitViewModel.assignmentUUID.value,
                uuid(),
                false,
                urls,
                currentUserEmail(),
                Timestamp.now()
            )

            FirebaseFirestore.getInstance().collection("submissions")
                .document(submission.id)
                .set(submission)
                .continueWithTask {
                    val ref = Firebase.firestore
                        .document("assignments/${submitViewModel.assignmentUUID.value}")
                    val submissions =
                        assignmentsViewModel.getAssignment()!!.submissions + submission.id
                    ref.update("submissions", submissions)
                }
                .addOnSuccessListener {
                    activity ?: return@addOnSuccessListener
                    submitViewModel.viewModelScope.launch(Dispatchers.Main) {
                        loading.animate().alpha(0f)
                        MaterialAlertDialogBuilder(context, R.style.SuccessDialog)
                            .setTitle("Success!")
                            .setMessage("Your assignment was handed in successfully!")
                            .setIcon(R.drawable.ic_check_black_24dp)
                            .setOnCancelListener {
                                Navigation.navigate(R.id.assignmentFragment)
                                submitViewModel.imagesData = SafeLiveData(ArrayList())
                                view.loading.alpha = 1f
                            }
                            .show()
                    }
                }
                .addOnFailureListener {
                    activity ?: return@addOnFailureListener
                    submitViewModel.viewModelScope.launch(Dispatchers.Main) {
                        MaterialAlertDialogBuilder(context, R.style.ErrorDialog)
                            .setTitle("An error occured.")
                            .setIcon(R.drawable.ic_error_outline_black_24dp)
                            .setMessage(it.message)
                            .setOnDismissListener {
                                NavHostFragment.findNavController(
                                    this@LoadingFragment
                                ).navigateUp()
                            }.show()
                        setLoadingText(it.message.toString(), view.loading_text)
                    }
                }
        }

        return view
    }

    override fun onDestroy() {
        super.onDestroy()
        coroutineJob.cancel()
    }

    private suspend fun setLoadingText(s: String, loadingTextView: TextView) {
        withContext(Dispatchers.Main) {
            loadingTextView.text = s
        }
    }

    companion object {
        fun newInstance(): LoadingFragment {
            return LoadingFragment()
        }
    }
}
