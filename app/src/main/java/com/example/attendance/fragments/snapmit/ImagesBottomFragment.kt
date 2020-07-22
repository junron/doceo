package com.example.attendance.fragments.snapmit

import android.graphics.Rect
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import com.example.attendance.adapters.snapmit.ImageAdapter
import com.example.attendance.util.android.SafeLiveData
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.android.synthetic.main.fragment_image_bottom.view.*
import java.io.File

class ImagesBottomFragment : BottomSheetDialogFragment() {
    lateinit var images: SafeLiveData<List<File>>
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view: View =
            inflater.inflate(R.layout.fragment_image_bottom, container, false)
        val recyclerView = view.image_recycler
        val lm =
            LinearLayoutManager(inflater.context, RecyclerView.VERTICAL, false)
        lm.stackFromEnd = false
        recyclerView.layoutManager = lm
        recyclerView.adapter =
            ImageAdapter(images)
        recyclerView.addItemDecoration(
            object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: Rect, view: View,
                    parent: RecyclerView, state: RecyclerView.State
                ) {
                    outRect.bottom = 32

                    // Add top margin only for the first item to avoid double space between items
                    if (parent.getChildLayoutPosition(view) == 0) {
                        outRect.top = 32
                    } else {
                        outRect.top = 0
                    }
                }
            }
        )
        if (images.value.isNotEmpty()) view.no_images.visibility = View.GONE
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.TransparentSheetTheme)
    }

    companion object {
        fun newInstance(images: SafeLiveData<List<File>>): ImagesBottomFragment {
            val frag = ImagesBottomFragment()
            frag.images = images
            return frag
        }
    }
}
