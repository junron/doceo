package com.example.attendance.util.android

import android.app.Activity
import android.content.Context
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder


fun hideKeyboard(activity: Activity) {
    val imm: InputMethodManager =
        activity.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    //Find the currently focused view, so we can grab the correct window token from it.
    var view: View? = activity.currentFocus
    //If no view currently has focus, create a new one, just so we can grab a window token from it
    if (view == null) {
        view = View(activity)
    }
    imm.hideSoftInputFromWindow(view.windowToken, 0)
}

operator fun TextView.plusAssign(s: String) {
    this.text = this.text.toString() + s
}

fun View.setMargin(left: Int, top: Int, right: Int, bottom: Int) {
    if (layoutParams == null) layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    this.layoutParams = CoordinatorLayout.LayoutParams(layoutParams)
        .apply {
            this.setMargins(left, top, right, bottom)
        }
}

private var dialog: AlertDialog? = null

fun Context.requestInputDialog(
    title: String,
    placeholder: String = "",
    confirm: String = "Confirm",
    callback: (String?) -> Unit
) {
    val editText = EditText(this).apply {
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
        setText(placeholder)
    }
    val confirmBtn =
        MaterialButton(this)
            .apply {
                setMargin(10, 8, 10, 8)
                text = confirm
                setOnClickListener {
                    callback(editText.text.toString())
                    dialog?.hide()
                }
            }
    editText.onTextChange {
        if (it.isBlank()) {
            confirmBtn.isEnabled = false
        }
    }

    val view = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
        addView(LinearLayout(context).apply {
            layoutParams = ViewGroup.LayoutParams(MATCH_PARENT, MATCH_PARENT)
            orientation = LinearLayout.VERTICAL
            setMargin(16, 16, 16, 16)
            addView(editText)
            addView(LinearLayout(context).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.END
                addView(
                    MaterialButton(context).apply {
                        setMargin(10, 8, 10, 8)
                        text = "Cancel"
                        setOnClickListener {
                            callback(null)
                            dialog?.hide()
                        }
                    })
                addView(confirmBtn)
            })
        })
    }
    dialog = MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setView(view)
        .show()
}
