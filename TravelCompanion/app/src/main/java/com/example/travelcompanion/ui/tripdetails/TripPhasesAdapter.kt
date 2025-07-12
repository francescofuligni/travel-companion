package com.example.travelcompanion.ui.tripdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.TripPhase
import java.text.SimpleDateFormat
import java.util.*

class TripPhasesAdapter(
    private val onPhaseClick: (TripPhase) -> Unit
) : RecyclerView.Adapter<TripPhasesAdapter.TripPhaseViewHolder>() {

    private var phases: List<TripPhase> = emptyList()

    fun updatePhases(newPhases: List<TripPhase>) {
        phases = newPhases
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TripPhaseViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_phase, parent, false)
        return TripPhaseViewHolder(view)
    }

    override fun onBindViewHolder(holder: TripPhaseViewHolder, position: Int) {
        holder.bind(phases[position])
    }

    override fun getItemCount(): Int = phases.size

    inner class TripPhaseViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvPhaseOrder: TextView = itemView.findViewById(R.id.tvPhaseOrder)
        private val tvPhaseTimestamp: TextView = itemView.findViewById(R.id.tvPhaseTimestamp)

        fun bind(phase: TripPhase) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            tvPhaseOrder.text = "Fase ${phase.phaseOrder}"
            tvPhaseTimestamp.text = dateFormat.format(Date(phase.timestamp))

            itemView.setOnClickListener {
                onPhaseClick(phase)
            }
        }
    }
}
