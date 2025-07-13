package com.example.travelcompanion.ui.tripdetails

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.Image
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy

/**
 * Adapter per visualizzare le immagini del viaggio in una griglia/lista orizzontale
 * Utilizza Glide per il caricamento efficiente delle immagini
 */
class TripImagesAdapter(
    private val onImageClick: (Image) -> Unit
) : RecyclerView.Adapter<TripImagesAdapter.ImageViewHolder>() {

    private var images: List<Image> = emptyList()

    /**
     * Aggiorna la lista delle immagini e notifica i cambiamenti
     */
    fun updateImages(newImages: List<Image>) {
        images = newImages
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_image, parent, false)
        return ImageViewHolder(view)
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    /**
     * ViewHolder per gli elementi immagine
     */
    inner class ImageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageView: ImageView = itemView.findViewById(R.id.ivTripImage)

        /**
         * Collega l'immagine alla view usando Glide
         */
        fun bind(image: Image) {
            // Usa Glide per caricare l'immagine con cache e placeholder
            Glide.with(itemView.context)
                .load(Uri.parse(image.uri))
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop()
                .placeholder(R.drawable.ic_placeholder_image)
                .error(R.drawable.ic_error_image)
                .into(imageView)

            itemView.setOnClickListener {
                onImageClick(image)
            }
        }
    }
}
