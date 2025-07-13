package com.example.travelcompanion.ui.newtrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentNewTripActiveBinding

/**
 * Fragment mostrato quando c'è già un viaggio attivo
 * Informa l'utente che può avere solo un viaggio attivo alla volta
 */
class NewTripActiveFragment : Fragment() {

    private var _binding: FragmentNewTripActiveBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTripActiveBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        /**
         * Naviga alla home per vedere il viaggio attivo
         */
        binding.btnStartNewTrip.setOnClickListener {
            findNavController().navigate(R.id.nav_home)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}