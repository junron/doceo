package com.example.attendance.util.android

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText

class TextChangedListener(private val editText: EditText, val callback: (text: String) -> Unit) :
    TextWatcher {
    fun setText(text: String) {
        editText.removeTextChangedListener(this)
        editText.setText(text)
        editText.addTextChangedListener(this)
    }

    override fun afterTextChanged(s: Editable) {}

    override fun beforeTextChanged(
        s: CharSequence, start: Int,
        count: Int, after: Int
    ) {
    }

    override fun onTextChanged(
        s: CharSequence, start: Int,
        before: Int, count: Int
    ) {
        callback(s.toString())
    }
}

fun EditText.onTextChange(callback: (text: String) -> Unit): TextChangedListener {
    val watcher = TextChangedListener(this, callback)
    this.addTextChangedListener(watcher)
    return watcher
}
