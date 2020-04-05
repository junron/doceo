package com.example.attendance.controllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import com.example.attendance.adapters.FilterAdapter
import com.example.attendance.adapters.ImmutableClasslistAdapter
import com.example.attendance.models.FilterParam
import com.example.attendance.models.Students
import com.example.attendance.util.android.hideKeyboard
import com.example.attendance.util.android.onTextChange
import kotlinx.android.synthetic.main.fragment_filter.*

object FilterController : FragmentController() {
    lateinit var constraints: String
    private lateinit var adapter: FilterAdapter
    var callback: ((String) -> Unit)? = null

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            adapter = FilterAdapter(filterEditText)
            suggestions.adapter = adapter
            adapter.updateFilters(if (::constraints.isInitialized) constraints else "")
            filterEditText.requestFocus()
            val classListAdapter = ImmutableClasslistAdapter(Students.students)
            classListView.adapter = classListAdapter
            classListView.layoutManager = GridLayoutManager(context.context!!, 2)
            filterEditText.onTextChange {
                adapter.updateFilters(it)
                if (callback == null) {
                    constraints = it
                }
                if (isValidFilter(it)) {
                    suggestions.visibility = GONE
                    classListView.visibility = VISIBLE
                    if (Students
                            .filterStudents(it.split(" "), Students.students)
                            .isNotEmpty()
                    ) filterDone.visibility = VISIBLE
                    classListAdapter.filterStudents(it.split(" "))
                } else {
                    classListView.visibility = GONE
                    suggestions.visibility = VISIBLE
                    filterDone.visibility = GONE
                }
            }
            filterDone.setOnClickListener {
                hideKeyboard(this.activity!!)
                callback?.invoke(filterEditText.text.toString())
                callback = null
            }
        }
    }

    private fun isValidFilter(constraints: String): Boolean {
        if (constraints.isBlank()) return false
        var hasValidConstraints = false
        constraints.split(" ")
            .forEach {
                if (it.isBlank()) return@forEach
                if (":" !in it) return false
                val (key, value) = it.split(":")
                val filterParam = FilterParam.filterParams.firstOrNull { param ->
                    param.key == "$key: "
                } ?: return false
                if (value !in filterParam.possibleValues) return false
                hasValidConstraints = true
            }
        return hasValidConstraints
    }

    override fun restoreState() {
        if (!::constraints.isInitialized) return
        with(context) {
            filterEditText.setText(constraints)
            adapter.updateFilters(constraints)
        }
    }
}
