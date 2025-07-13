package com.example.travelcompanion.ui.tripdetails

import android.net.Uri
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.databinding.ItemTripImageBinding

/**
 * Adapter per visualizzare le immagini del viaggio in una griglia/lista orizzontale
 */
class TripImagesAdapter(
    private val onImageClick: (Image) -> Unit
) : ListAdapter<Image, TripImagesAdapter.ImageViewHolder>(DiffCallback) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val binding = ItemTripImageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ImageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ImageViewHolder(private val binding: ItemTripImageBinding) : RecyclerView.ViewHolder(binding.root) {

        fun bind(image: Image) {
            // Usa Glide per caricare l'immagine
            Glide.with(binding.root.context)
                .load(Uri.parse(image.uri))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .into(binding.ivTripImage)

            binding.root.setOnClickListener {
                onImageClick(image)
            }
        }
    }

    companion object DiffCallback : DiffUtil.ItemCallback<Image>() {
        override fun areItemsTheSame(oldItem: Image, newItem: Image) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Image, newItem: Image) = oldItem == newItem
    }
}
