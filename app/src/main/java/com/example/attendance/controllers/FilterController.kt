package com.example.attendance.controllers

import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.fragment.app.Fragment
import com.example.attendance.R
import com.example.attendance.adapters.FilterAdapter
import com.example.attendance.models.FilterParam
import com.example.attendance.util.android.Navigation
import com.example.attendance.util.android.hideKeyboard
import com.example.attendance.util.android.onTextChange
import kotlinx.android.synthetic.main.fragment_filter.*

object FilterController : FragmentController() {
    private lateinit var constraints: String
    private lateinit var adapter: FilterAdapter

    override fun init(context: Fragment) {
        super.init(context)
        with(context) {
            adapter = FilterAdapter(filterEditText)
            suggestions.adapter = adapter
            adapter.updateFilters("")
            filterEditText.requestFocus()
            filterEditText.onTextChange {
                adapter.updateFilters(it)
                constraints = it
                if (isValidFilter(it)) {
                    filterDone.visibility = VISIBLE
                } else {
                    filterDone.visibility = GONE
                }
            }
            back.setOnClickListener {
                hideKeyboard(this.activity!!)
                if (filterEditText.text.toString().isBlank()) {
                    MainController.updateFilters("")
                }
                Navigation.navigate(R.id.mainContent)
            }
            filterDone.setOnClickListener {
                hideKeyboard(this.activity!!)
                Navigation.navigate(R.id.mainContent)
                MainController.updateFilters(filterEditText.text.toString())
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
