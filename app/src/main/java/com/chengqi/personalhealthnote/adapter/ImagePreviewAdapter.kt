package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chengqi.personalhealthnote.R
import com.chengqi.personalhealthnote.widget.TouchImageView
import java.io.File

class ImagePreviewAdapter(
    private val imagePaths: List<String>
) : RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivFullscreen: TouchImageView = itemView.findViewById(R.id.ivFullscreen)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_fullscreen, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val path = imagePaths[position]
        Glide.with(holder.itemView.context)
            .load(File(path))
            .fitCenter()
            .into(holder.ivFullscreen)
    }

    override fun getItemCount(): Int = imagePaths.size
}
