package com.example.attendance.adapters.snapmit;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.attendance.R;

import java.util.List;

public class ImagesAdapter extends RecyclerView.Adapter<ImagesAdapter.Card> {
    List<String> images;
    Fragment fragment;

    public ImagesAdapter(List<String> images, Fragment fragment) {
        Log.d("IMAGES", images.toString());
        this.images = images;
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public Card onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.image_adapter_item, parent, false);
        return new Card(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Card holder, int position) {
        holder.itemView.findViewById(R.id.image).setTag(position);
        Glide.with(fragment).load(images.get(position)).placeholder(R.drawable.ic_placeholder).into((ImageView) holder.itemView.findViewById(R.id.image));
    }

    public void resetImage(ImageView image, int position) {
        Glide.with(fragment).load(images.get(position)).placeholder(R.drawable.ic_placeholder).into(image);
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public class Card extends RecyclerView.ViewHolder {
        public Card(@NonNull View itemView) {
            super(itemView);
        }
    }
}
