package com.example.attendance.adapters

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.controllers.NewClasslistGroupController
import com.example.attendance.models.Tag
import com.example.attendance.models.Tags
import com.example.attendance.util.android.Preferences
import com.example.attendance.util.android.onTextChange
import com.example.attendance.util.uuid
import kotlinx.android.synthetic.main.tag_item.view.*
import petrov.kristiyan.colorpicker.ColorPicker


class TagAdapter(var tags: MutableList<Tag>, private val editable: Boolean) :
    BaseAdapter() {
    init {
        if (editable) {
            tags.plusAssign(Tag(uuid(), "Tag name", -1))
        }
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        return LayoutInflater.from(parent.context).inflate(R.layout.tag_item, null).apply {
            if (item.color == -1) {
                tagName.setText("")
                tagName.hint = "Add tag"
                tagName.isFocusable = false
                tagName.isFocusableInTouchMode = false
                tagColor.setImageResource(R.drawable.ic_baseline_palette_24)
                tagColor.setColorFilter(
                    if (Preferences.isDarkMode())
                        Color.WHITE
                    else Color.BLACK
                )
                tagDelete.visibility = View.GONE
            } else {
                tagName.setText(item.name)
                tagColor.setColorFilter(item.color)
            }
            if (editable) {
                tagColor.setOnClickListener {
                    showColorPicker(position, item)
                }
                tagName.setOnClickListener {
                    if (tagName.hint == "Add tag") showColorPicker(position, item)
                }
                tagName.onTextChange {
                    tags[position] = item.copy(name = it)
                }
                tagDelete.setOnClickListener {
                    tags.remove(item)
                    notifyDataSetChanged()
                }
                if (item.id in Tags.defaultTags.map { it.id }) tagDelete.visibility = View.GONE
            } else {
                tagName.isFocusable = false
                tagName.isFocusableInTouchMode = false
                tagDelete.visibility = View.GONE
            }
        }
    }

    fun showColorPicker(position: Int, item: Tag) {
        val colorPicker = ColorPicker(MainActivity.activity)
        colorPicker.negativeButton.setTextColor(
            if (Preferences.isDarkMode())
                Color.WHITE
            else Color.BLACK
        )
        colorPicker.setOnChooseColorListener(object :
            ColorPicker.OnChooseColorListener {
            override fun onChooseColor(_p: Int, color: Int) {
                tags[position] = item.copy(color = color)
                notifyDataSetChanged()
            }

            override fun onCancel() {
            }

        })
        colorPicker.show()
    }

    override fun getItem(position: Int) = tags[position]

    override fun getItemId(position: Int) = 0L

    override fun getCount() = tags.size

    override fun notifyDataSetChanged() {
        if (editable && tags.find { it.color == -1 } == null) {
            tags.plusAssign(Tag(uuid(), "Tag name", -1))
        }
        NewClasslistGroupController.checkValidState()
        super.notifyDataSetChanged()
    }
}
