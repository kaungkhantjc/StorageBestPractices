package com.jcsamples.storagebestpractices.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jcsamples.storagebestpractices.R
import com.jcsamples.storagebestpractices.databinding.ItemImageBinding
import com.jcsamples.storagebestpractices.models.ImageModel

class ImageAdapter(
    private val images: MutableList<ImageModel>,
    private val onClick: (ImageModel) -> Unit
) :
    RecyclerView.Adapter<ImageAdapter.PlaceHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaceHolder {
        return PlaceHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_image, parent, false)
        )
    }

    override fun onBindViewHolder(holder: PlaceHolder, position: Int) {
        val imageModel = images[position]
        holder.binding.tvName.text = imageModel.displayName
        Glide.with(holder.binding.iv)
            .load(imageModel.contentUri)
            .into(holder.binding.iv)

        holder.binding.root.setOnClickListener { onClick.invoke(imageModel) }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    class PlaceHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val binding = ItemImageBinding.bind(itemView)
    }
}