package com.example.attendance.adapters.snapmit

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.attendance.MainActivity
import com.example.attendance.R
import com.example.attendance.util.android.SafeLiveData
import kotlinx.android.synthetic.main.image_card.view.*
import java.io.File

internal class ImageAdapter(var images: SafeLiveData<List<File>>) :
    RecyclerView.Adapter<ImageAdapter.Card>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): Card {
        val card: View =
            LayoutInflater.from(parent.context).inflate(
                R.layout.image_card, parent, false
            )
        return Card(card)
    }

    override fun onBindViewHolder(
        holder: Card,
        position: Int
    ) {
        val target =
            holder.view.image
        val imageValue = images.value
        Glide.with(MainActivity.activity)
            .load(imageValue[imageValue.lastIndex - position].path)
            .diskCacheStrategy(DiskCacheStrategy.NONE)
            .placeholder(R.drawable.ic_placeholder)
            .into(target)
        holder.view.remove_button
            .setOnClickListener {
                val newVal = images.value.toMutableList()
                newVal.removeAt(newVal.lastIndex - position)
                images.postValue(newVal)
                notifyDataSetChanged()
            }
    }

    override fun getItemCount(): Int {
        Log.d("imagesSize", images.value.size.toString())
        return images.value.size
    }

    internal class Card(var view: View) :
        RecyclerView.ViewHolder(view)

}
