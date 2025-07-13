package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentHomeBinding
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.launch

/**
 * Fragment principale della home che decide se mostrare un viaggio attivo o meno
 */
class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private fun showNoTrip() {
        childFragmentManager.beginTransaction()
            .replace(R.id.home_container, HomeNoTripFragment())
            .commit()
    }

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
        loadActiveTrip()
    }

    override fun onResume() {
        super.onResume()
        // Refresh when returning to home
        loadActiveTrip()
    }

    fun forceReload() {
        loadActiveTrip()  // o qualunque metodo tu usi per caricare dinamicamente il fragment figlio
    }

    /**
     * Carica il viaggio attivo se presente
     */
    private fun loadActiveTrip() {
        val repository = TravelRepository.create(requireContext())
        lifecycleScope.launch {
            try {
                val activeId = repository.getActiveTripId()
                Log.d("HomeFragment", "Active trip ID: $activeId")

                val trip = activeId?.let { repository.getTripById(it) }
                Log.d("HomeFragment", "Active trip: $trip")

                val fragment = if (trip != null && trip.isActive) {
                    HomeActiveTripFragment().apply {
                        arguments = Bundle().apply { putLong("tripId", trip.id) }
                    }
                } else {
                    null
                }
                childFragmentManager.beginTransaction()
                    .replace(R.id.home_container, fragment ?: HomeNoTripFragment())
                    .commit()
            } catch (e: Exception) {
                Log.e("HomeFragment", "Error loading active trip", e)
                showNoTrip()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}