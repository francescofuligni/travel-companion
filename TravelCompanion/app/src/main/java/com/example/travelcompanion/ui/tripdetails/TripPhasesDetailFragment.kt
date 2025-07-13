package com.example.travelcompanion.ui.tripdetails

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import com.example.travelcompanion.database.models.TripPhase
import com.example.travelcompanion.databinding.FragmentTripPhasesDetailBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripPhasesDetailFragment @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: FragmentTripPhasesDetailBinding

    init {
        binding = FragmentTripPhasesDetailBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
    }

    fun setTripPhase(phase: TripPhase) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvPhaseTimestamp.text = dateFormat.format(Date(phase.timestamp))
        binding.tvPhaseOrder.text = "Fase ${phase.phaseOrder}"

        // Hide note and image sections since they're now separate
        binding.tvPhaseNote.visibility = View.GONE
        binding.tvNoteLabel.visibility = View.GONE
        binding.ivPhaseImage.visibility = View.GONE
    }

    fun setOnEditClickListener(onEditNote: () -> Unit, onEditImage: () -> Unit) {
        binding.btnEditNote.setOnClickListener { onEditNote() }
        binding.btnEditImage.setOnClickListener { onEditImage() }
    }
}
