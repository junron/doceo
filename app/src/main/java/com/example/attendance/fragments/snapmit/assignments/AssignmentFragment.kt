package com.example.attendance.fragments.snapmit.assignments

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.ProgressBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.AssignmentSubmissionAdapter
import com.example.attendance.util.android.SpacesItemDecoration
import com.example.attendance.util.android.onTextChange
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.delete_dialog.view.*
import kotlinx.android.synthetic.main.fragment_assignments2.view.*

class AssignmentFragment : Fragment() {
    private val assignmentsViewModel: AssignmentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_assignments2, container, false)
        // NPE if assignment doesn't exist
        val assignment = assignmentsViewModel.getAssignment()!!
        root.name.text = assignment.name
        val recycler = root.recycler
        val llm = LinearLayoutManager(root.context)
        llm.orientation = RecyclerView.VERTICAL
        recycler.layoutManager = llm
        recycler.adapter = AssignmentSubmissionAdapter(
            assignmentsViewModel,
            root.loading,
            root.no_items
        )
        recycler.addItemDecoration(SpacesItemDecoration(64))
        root.delete_button
            .setOnClickListener { v: View ->
                val view =
                    LayoutInflater.from(v.context).inflate(R.layout.delete_dialog, null)
                val confirmDialog =
                    MaterialAlertDialogBuilder(v.context, R.style.ErrorDialog)
                        .setTitle("Delete assignment")
                        .setIcon(R.drawable.ic_delete_black_24dp)
                        .setView(view)
                        .create()
                val name: String = assignment.name
                view.assignment_name.text = name
                view.assignment_delete_button.setOnClickListener { v1: View ->
                    confirmDialog.dismiss()
                    if (!v1.isClickable) return@setOnClickListener
                    val imm =
                        requireActivity().getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        v1.windowToken,
                        InputMethodManager.RESULT_UNCHANGED_SHOWN
                    )
                    val spinner = ProgressBar(v.context)
                    spinner.setPadding(16, 16, 16, 16)
                    val dialog =
                        MaterialAlertDialogBuilder(v.context, R.style.MyDialog)
                            .setTitle("Please wait")
                            .setView(spinner)
                            .show()
                    dialog.setCancelable(false)
                    dialog.setCanceledOnTouchOutside(false)
                    dialog.show()
                    assignmentsViewModel.deleteAssignment { task ->
                        val dialogBuilder: MaterialAlertDialogBuilder
                        if (task.isSuccessful) {
                            dialogBuilder = MaterialAlertDialogBuilder(
                                v.context,
                                R.style.SuccessDialog
                            )
                                .setIcon(R.drawable.ic_check_black_24dp)
                                .setTitle("Success!")
                                .setOnCancelListener {
                                    NavHostFragment.findNavController(
                                        this
                                    ).navigateUp()
                                }
                        } else {
                            dialogBuilder = MaterialAlertDialogBuilder(
                                v.context,
                                R.style.ErrorDialog
                            )
                            dialogBuilder.setIcon(R.drawable.ic_error_outline_black_24dp)
                            dialogBuilder.setTitle("Something went wrong.")
                        }
                        dialogBuilder.show()
                    }
                }
                view.assignment_delete_button.isClickable = false
                view.editText.onTextChange {
                    with(view.assignment_delete_button) {
                        if (it == name) {
                            isClickable = true
                            setBackgroundColor(resources.getColor(R.color.errorRed))
                        } else {
                            isClickable = false
                            setBackgroundColor(6447714)
                        }
                    }
                }
                confirmDialog.show()
            }
        return root
    }

    companion object {
        fun newInstance(): AssignmentFragment {
            val fragment = AssignmentFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}
