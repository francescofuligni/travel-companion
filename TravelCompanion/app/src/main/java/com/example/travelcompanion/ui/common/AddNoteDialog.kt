package com.example.travelcompanion.ui.common

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.example.travelcompanion.R
import com.google.android.material.textfield.TextInputEditText

class AddNoteDialog : DialogFragment() {
    
    private var listener: ((String) -> Unit)? = null
    private var existingNote: String? = null
    
    fun setOnNoteAddedListener(listener: (String) -> Unit) {
        this.listener = listener
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.dialog_add_note, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val etNote = view.findViewById<TextInputEditText>(R.id.etNote)
        // Limite massimo di 500 caratteri
        etNote.filters = arrayOf(InputFilter.LengthFilter(500))
        val btnSave = view.findViewById<Button>(R.id.btnSaveNote)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelNote)
        
        existingNote?.let { etNote.setText(it) }
        
        btnSave.setOnClickListener {
            val noteText = etNote.text.toString()
            when {
                noteText.isBlank() -> {
                    Toast.makeText(requireContext(), getString(R.string.error_empty), Toast.LENGTH_SHORT).show()
                }
                noteText.length > 500 -> {
                    Toast.makeText(requireContext(), getString(R.string.error_too_long), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    listener?.invoke(noteText.trim())
                    dismiss()
                }
            }
        }
        
        btnCancel.setOnClickListener {
            dismiss()
        }
    }
    
    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }
}
