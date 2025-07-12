package com.example.travelcompanion.ui.common

import android.content.Context
import android.net.Uri
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.ImageView
import android.widget.TextView
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.TripPhase
import com.example.travelcompanion.databinding.ViewTripPhaseDetailsBinding
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TripPhaseDetailsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewTripPhaseDetailsBinding

    init {
        binding = ViewTripPhaseDetailsBinding.inflate(LayoutInflater.from(context), this, true)
        orientation = VERTICAL
    }

    fun setTripPhase(phase: TripPhase) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        binding.tvPhaseTimestamp.text = dateFormat.format(Date(phase.timestamp))
        binding.tvPhaseOrder.text = "Fase ${phase.phaseOrder}"

        // Mostra la nota se presente
        if (!phase.note.isNullOrEmpty()) {
            binding.tvPhaseNote.text = phase.note
            binding.tvPhaseNote.visibility = View.VISIBLE
            binding.tvNoteLabel.visibility = View.VISIBLE
        } else {
            binding.tvPhaseNote.visibility = View.GONE
            binding.tvNoteLabel.visibility = View.GONE
        }

        // Mostra l'immagine se presente
        if (!phase.imageUri.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(phase.imageUri)
                binding.ivPhaseImage.setImageURI(imageUri)
                binding.ivPhaseImage.visibility = View.VISIBLE
            } catch (e: Exception) {
                binding.ivPhaseImage.visibility = View.GONE
            }
        } else {
            binding.ivPhaseImage.visibility = View.GONE
        }
    }

    fun setOnEditClickListener(onEditNote: () -> Unit, onEditImage: () -> Unit) {
        binding.btnEditNote.setOnClickListener { onEditNote() }
        binding.btnEditImage.setOnClickListener { onEditImage() }
    }
}
