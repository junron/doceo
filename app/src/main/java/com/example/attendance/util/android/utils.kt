package com.example.attendance.util.android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.menu.MenuBuilder
import androidx.appcompat.widget.PopupMenu
import com.example.attendance.R
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.android.synthetic.main.dialog_layout.view.*


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
    (this.layoutParams as ViewGroup.MarginLayoutParams)
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
    val view = LayoutInflater.from(this).inflate(R.layout.dialog_layout, null)
        .apply {
            dialogEditText.setText(placeholder)
            dialogEditText.onTextChange {
                dialogConfirm.isEnabled = !it.isBlank()
            }
            dialogConfirm.text = confirm
            dialogConfirm.setOnClickListener {
                callback(dialogEditText.text.toString())
                dialog?.hide()
                dialog = null
            }
            dialogCancel.setOnClickListener {
                callback(null)
                dialog?.hide()
                dialog = null
            }
        }
    dialog = MaterialAlertDialogBuilder(this)
        .setTitle(title)
        .setView(view)
        .show()
}

@SuppressLint("RestrictedApi")
fun PopupMenu.showIcons() {
    (menu as MenuBuilder).setOptionalIconsVisible(true)
}
