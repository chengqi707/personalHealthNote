package com.chengqi.personalhealthnote.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.chengqi.personalhealthnote.R

/**
 * 图片预览适配器
 * 展示已选择的图片缩略图，支持删除和点击查看
 */
class ImageAdapter(
    private val imagePaths: MutableList<String>,
    private val onImageClick: (Int) -> Unit,
    private val onImageDelete: (Int) -> Unit
) : RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_image_preview, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imagePath = imagePaths[position]

        // 使用Glide加载图片
        Glide.with(holder.itemView.context)
            .load(imagePath)
            .centerCrop()
            .into(holder.ivImage)

        // 图片点击事件
        holder.ivImage.setOnClickListener {
            onImageClick(position)
        }

        // 删除按钮点击事件
        holder.ivDelete.setOnClickListener {
            onImageDelete(position)
        }
    }

    override fun getItemCount(): Int = imagePaths.size

    /**
     * 添加图片
     */
    fun addImage(path: String) {
        imagePaths.add(path)
        notifyItemInserted(imagePaths.size - 1)
    }

    /**
     * 添加多张图片
     */
    fun addImages(paths: List<String>) {
        val start = imagePaths.size
        imagePaths.addAll(paths)
        notifyItemRangeInserted(start, paths.size)
    }

    /**
     * 删除图片
     */
    fun removeImage(position: Int) {
        if (position >= 0 && position < imagePaths.size) {
            imagePaths.removeAt(position)
            notifyItemRemoved(position)
            notifyItemRangeChanged(position, imagePaths.size - position)
        }
    }

    /**
     * 获取所有图片路径
     */
    fun getImagePaths(): List<String> = imagePaths.toList()

    class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val ivImage: ImageView = itemView.findViewById(R.id.ivImage)
        val ivDelete: ImageView = itemView.findViewById(R.id.ivDelete)
    }
}
