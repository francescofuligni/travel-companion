package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.addTextChangedListener
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.databinding.FragmentNoteBinding

class NoteFragment : Fragment() {

    private var _binding: FragmentNoteBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoteBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Disabilita il bottone Salva finché i campi sono vuoti
        binding.btnSaveNote.isEnabled = false

        // Abilita il bottone quando entrambi i campi non sono vuoti
        binding.etNoteTitle.addTextChangedListener { updateSaveButtonState() }
        binding.etNoteContent.addTextChangedListener { updateSaveButtonState() }

        binding.btnSaveNote.setOnClickListener {
            val title = binding.etNoteTitle.text.toString()
            val content = binding.etNoteContent.text.toString()
            // TODO: gestire il salvataggio della nota
            findNavController().navigateUp()
        }
    }

    /**
     * Abilita o disabilita il bottone Salva in base al contenuto dei campi.
     */
    private fun updateSaveButtonState() {
        val title = binding.etNoteTitle.text.toString()
        val content = binding.etNoteContent.text.toString()
        binding.btnSaveNote.isEnabled = title.isNotBlank() && content.isNotBlank()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}