package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.IdRes
import androidx.fragment.app.Fragment
import androidx.navigation.findNavController
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentHomeNoTripBinding

private fun View.onClickNav(@IdRes actionId: Int) {
    findNavController().navigate(actionId)
}

class HomeNoTripFragment : Fragment() {

    private var _binding: FragmentHomeNoTripBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeNoTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnStartNewTrip.onClickNav(R.id.nav_new_trip)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}