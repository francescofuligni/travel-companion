package com.example.travelcompanion.ui.mytrips

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.TripType
import com.bumptech.glide.Glide

/**
 * Modello UI per rappresentare un viaggio nella lista
 * Contiene tutti i dati necessari per la visualizzazione
 */
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

/**
 * Adapter per la RecyclerView che visualizza la lista dei viaggi
 * Gestisce la visualizzazione e l'interazione con gli elementi della lista
 */
class MyTripsAdapter(
    private var items: List<TripUiModel>,
    private val onTripClicked: (Long) -> Unit
) : RecyclerView.Adapter<MyTripsAdapter.TripViewHolder>() {

    /**
     * ViewHolder per gli elementi della lista viaggi
     * Contiene i riferimenti alle view e gestisce il binding dei dati
     */
    inner class TripViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val ivTripImage: ImageView = view.findViewById(R.id.ivTripImage)
        private val tvTripTitle: TextView = view.findViewById(R.id.tvTripTitle)
        private val tvTripDestination: TextView = view.findViewById(R.id.tvTripDestination)

        /**
         * Collega i dati del viaggio alle view
         */
        fun bind(item: TripUiModel) {
            tvTripTitle.text = item.title
            tvTripDestination.text = item.destination
            
            if (!item.imageUrl.isNullOrBlank()) {
                Glide.with(itemView.context)
                    .load(item.imageUrl)
                    .placeholder(R.drawable.missing_img)
                    .error(R.drawable.missing_img)
                    .centerCrop()
                    .into(ivTripImage)
            } else {
                ivTripImage.setImageResource(R.drawable.missing_img)
            }
            
            itemView.setOnClickListener {
                onTripClicked(item.id)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip, parent, false)
        return TripViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    /**
     * Aggiorna la lista dei viaggi e notifica i cambiamenti
     */
    fun submitList(newItems: List<TripUiModel>) {
        items = newItems
        notifyDataSetChanged()
    }
}