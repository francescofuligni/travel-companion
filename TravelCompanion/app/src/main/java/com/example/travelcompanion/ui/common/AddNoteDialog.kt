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
import com.example.travelcompanion.databinding.DialogAddNoteBinding
import com.google.android.material.textfield.TextInputEditText

private var _binding: DialogAddNoteBinding? = null
private val binding get() = _binding!!

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
        _binding = DialogAddNoteBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val etNote = binding.etNote.apply {
            filters = arrayOf(InputFilter.LengthFilter(500))
        }
        val btnSave = binding.btnSaveNote
        val btnCancel = binding.btnCancelNote
        
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
