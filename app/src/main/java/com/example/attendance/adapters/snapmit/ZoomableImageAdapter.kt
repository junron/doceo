package com.example.attendance.adapters.snapmit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.attendance.R
import kotlinx.android.synthetic.main.zoomable_image.view.*

class ZoomableImageAdapter(
    images: List<String>,
    fragment: Fragment
) : RecyclerView.Adapter<ZoomableImageAdapter.Card>() {
    var images: List<String>
    var fragment: Fragment
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Card {
        val view: View = LayoutInflater.from(parent.context)
            .inflate(R.layout.zoomable_image, parent, false)
        return Card(view)
    }

    override fun onBindViewHolder(
        holder: Card,
        position: Int
    ) {
        holder.itemView.image.tag = position
        Glide.with(fragment).load(images[position]).placeholder(R.drawable.ic_placeholder)
            .into(holder.itemView.image)
    }

    fun resetImage(image: ImageView, position: Int) {
        Glide.with(fragment).load(images[position]).placeholder(R.drawable.ic_placeholder)
            .into(image)
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class Card(itemView: View) : RecyclerView.ViewHolder(itemView)

    init {
        Log.d("IMAGES", images.toString())
        this.images = images
        this.fragment = fragment
    }
}
