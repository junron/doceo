package com.example.attendance.adapters

import android.graphics.drawable.BitmapDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.attendance.R
import kotlinx.android.synthetic.main.onboard_image.view.*


internal class OnBoardingAdapter : RecyclerView.Adapter<OnBoardingAdapter.Image>() {
    private var images =
        intArrayOf(R.drawable.onboard1, R.drawable.onboard2, R.drawable.onboard3)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Image {
        return Image(
            LayoutInflater.from(parent.context).inflate(R.layout.onboard_image, parent, false)
        )
    }

    override fun onBindViewHolder(
        holder: Image,
        position: Int
    ) {
        with(holder.itemView) {
            (image.drawable as? BitmapDrawable)?.bitmap?.recycle()
            image.setImageResource(images[position])
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    inner class Image(itemView: View) : RecyclerView.ViewHolder(itemView)
}
