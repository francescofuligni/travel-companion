package com.example.travelcompanion.ui.mytrips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.databinding.ItemTripBinding

data class TripUiModel(
    val id: Long,
    val title: String,
    val destination: String,
    val imageUrl: String? = null,
    val startDate: Long? = null,
    val endDate: Long? = null,
    val duration: Double? = null,
    val distance: Double? = null,
    val type: TripType? = null
)

class MyTripsAdapter(
    private val onTripClicked: (Long) -> Unit
) : ListAdapter<TripUiModel, MyTripsAdapter.TripViewHolder>(DiffCallback) {

    inner class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val binding = ItemTripBinding.bind(view)

        init {
            itemView.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    onTripClicked(currentList[position].id)
                }
            }
        }

        fun bind(item: TripUiModel) {
            binding.tvTripTitle.text = item.title
            binding.tvTripDestination.text = item.destination
            val imageView = binding.ivTripImage
            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.missing_img)
                    .error(R.drawable.missing_img)
                    .centerCrop()
                    .into(imageView)
            } else {
                imageView.setImageResource(R.drawable.missing_img)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    companion object DiffCallback : DiffUtil.ItemCallback<TripUiModel>() {
        override fun areItemsTheSame(oldItem: TripUiModel, newItem: TripUiModel) = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: TripUiModel, newItem: TripUiModel) = oldItem == newItem
    }
}