package com.example.travelcompanion.ui.tripdetails

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.travelcompanion.R
import com.example.travelcompanion.database.models.Note
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter per visualizzare le note del viaggio
 */
class TripNotesAdapter(
    private val onNoteClick: (Note) -> Unit
) : RecyclerView.Adapter<TripNotesAdapter.NoteViewHolder>() {

    private var notes: List<Note> = emptyList()

    fun updateNotes(newNotes: List<Note>) {
        notes = newNotes
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_trip_note, parent, false)
        return NoteViewHolder(view)
    }

    override fun onBindViewHolder(holder: NoteViewHolder, position: Int) {
        holder.bind(notes[position])
    }

    override fun getItemCount(): Int = notes.size

    inner class NoteViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val tvNoteContent: TextView = itemView.findViewById(R.id.tvNoteContent)
        private val tvNoteTimestamp: TextView = itemView.findViewById(R.id.tvNoteTimestamp)

        fun bind(note: Note) {
            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            
            tvNoteContent.text = note.content
            tvNoteTimestamp.text = dateFormat.format(Date(note.timestamp))

            itemView.setOnClickListener {
                onNoteClick(note)
            }
        }
    }
}
