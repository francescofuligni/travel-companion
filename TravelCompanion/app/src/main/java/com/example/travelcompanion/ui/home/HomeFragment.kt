package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentHomeBinding
import com.example.travelcompanion.repository.TravelRepository

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = TravelRepository.create(requireContext())
        val factory = HomeVMFactory(repository)
        val viewModel = ViewModelProvider(this, factory)[HomeViewModel::class.java]
        viewModel.activeTrip.observe(viewLifecycleOwner) { trip ->
            val fragment = if (trip != null) {
                val bundle = Bundle().apply {
                    putLong("tripId", trip.id)
                }
                HomeActiveTripFragment().apply {
                    arguments = bundle
                }
            } else {
                HomeNoTripFragment()
            }

            childFragmentManager.beginTransaction()
                .replace(R.id.home_container, fragment)
                .commit()
        }

        viewModel.checkActiveTrip()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}