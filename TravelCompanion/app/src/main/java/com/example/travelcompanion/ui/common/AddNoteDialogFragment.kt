package com.example.travelcompanion.ui.common

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.example.travelcompanion.R
import com.google.android.material.textfield.TextInputEditText

class AddNoteDialogFragment : DialogFragment() {
    
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
        val btnSave = view.findViewById<Button>(R.id.btnSaveNote)
        val btnCancel = view.findViewById<Button>(R.id.btnCancelNote)
        
        existingNote?.let { etNote.setText(it) }
        
        btnSave.setOnClickListener {
            val note = etNote.text.toString().trim()
            if (note.isNotEmpty()) {
                listener?.invoke(note)
                dismiss()
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
