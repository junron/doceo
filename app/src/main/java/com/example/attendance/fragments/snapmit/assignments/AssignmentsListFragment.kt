package com.example.attendance.fragments.snapmit.assignments

import android.content.Context
import android.content.DialogInterface
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ProgressBar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.AssignmentAdapter
import com.example.attendance.models.snapmit.Assignment
import com.example.attendance.util.android.SpacesItemDecoration
import com.example.attendance.viewmodels.AssignmentsViewModel
import com.google.android.gms.tasks.Task
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.HttpsCallableResult
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.android.synthetic.main.fragment_assignments.view.*
import java.util.*

class AssignmentsListFragment : Fragment() {
    private val assignmentsViewModel: AssignmentsViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root =
            inflater.inflate(R.layout.fragment_assignments, container, false)
        val recyclerView: RecyclerView = root.findViewById(R.id.recycler)
        recyclerView.adapter =
            AssignmentAdapter(
                assignmentsViewModel,
                root.loading,
                root.fab,
                root.no_items
            )
        val llm = LinearLayoutManager(container!!.context)
        llm.orientation = RecyclerView.VERTICAL
        recyclerView.layoutManager = llm
        recyclerView.addItemDecoration(SpacesItemDecoration(64))
        root.fab
            .setOnClickListener { v: View ->
                val editText = EditText(v.context)
                editText.setTextColor(ContextCompat.getColor(requireContext(), R.color.textColor))
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    editText.isFocusedByDefault = true
                }
                val dialog =
                    MaterialAlertDialogBuilder(v.context, R.style.SuccessDialog)
                        .setIcon(R.drawable.ic_paper)
                        .setTitle("New Assignment")
                        .setMessage("Create a new assignment; But first, give it a name! This can't be changed later.")
                        .setView(editText)
                dialog.setPositiveButton(
                    "Ok"
                ) { _: DialogInterface?, _: Int ->
                    val imm = this.requireActivity()
                        .getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                    imm.hideSoftInputFromWindow(
                        editText.windowToken,
                        InputMethodManager.RESULT_UNCHANGED_SHOWN
                    )
                    val spinner = ProgressBar(v.context)
                    spinner.setPadding(16, 16, 16, 16)
                    val dialog1 =
                        MaterialAlertDialogBuilder(v.context, R.style.MyDialog)
                            .setTitle("Please wait")
                            .setView(spinner)
                            .show()
                    dialog1.setCancelable(false)
                    dialog1.setCanceledOnTouchOutside(false)
                    var name = editText.text.toString()
                    if (name.isEmpty()) name = "Unnamed assignment"
                    val data: MutableMap<String, Any> =
                        HashMap()
                    data["name"] = name
                    Log.d("CALL", "START")
                    val finalName = name
                    FirebaseFunctions
                        .getInstance()
                        .getHttpsCallable("createAssignment")
                        .call(data)
                        .continueWith { task: Task<HttpsCallableResult> ->
                            Log.d("CALL", "END")
                            dialog1.dismiss()
                            val dialogBuilder: MaterialAlertDialogBuilder
                            if (task.isSuccessful) {
                                val response =
                                    JsonParser.parseString(
                                        task.result!!.data as String?
                                    ) as JsonObject
                                val code =
                                    response["message"].asString
                                dialogBuilder = MaterialAlertDialogBuilder(
                                    v.context,
                                    R.style.SuccessDialog
                                )
                                dialogBuilder.setIcon(R.drawable.ic_check_black_24dp)
                                dialogBuilder.setTitle("Success!")
                                dialogBuilder.setOnDismissListener { dialog2: DialogInterface? ->
                                    (recyclerView.adapter as AssignmentAdapter?)!!.assignments.add(
                                        0,
                                        Assignment(
                                            code,
                                            false,
                                            finalName,
                                            listOf()
                                        )
                                    )
                                    root.findViewById<View>(R.id.no_items)
                                        .animate().alpha(0f)
                                    recyclerView.adapter!!.notifyItemInserted(0)
                                    recyclerView.layoutManager!!.scrollToPosition(0)
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
                            1
                        }
                }
                dialog.show()
            }
        return root
    }

    companion object {
        fun newInstance(): AssignmentsListFragment {
            val fragment = AssignmentsListFragment()
            val args = Bundle()
            fragment.arguments = args
            return fragment
        }
    }
}

