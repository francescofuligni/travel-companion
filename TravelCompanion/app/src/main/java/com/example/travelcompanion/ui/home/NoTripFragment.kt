

package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentNoTripBinding

class NoTripFragment : Fragment() {

    private var _binding: FragmentNoTripBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNoTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartNewTrip.setOnClickListener {
            findNavController().navigate(R.id.action_noTripFragment_to_nav_new_trip)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}