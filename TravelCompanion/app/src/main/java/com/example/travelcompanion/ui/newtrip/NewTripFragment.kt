package com.example.travelcompanion.ui.newtrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.commit
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.R
import com.example.travelcompanion.repository.TravelRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NewTripFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = TravelRepository.create(requireContext())

        lifecycleScope.launch {
            val activeTrip = withContext(Dispatchers.IO) {
                val activeId = repository.getActiveTripId()
                activeId?.let { repository.getTripById(it) }
            }

            val fragment = if (activeTrip != null && activeTrip.isActive) {
                NewTripActiveFragment()
            } else {
                NewTripCreateFragment()
            }

            childFragmentManager.commit {
                replace(R.id.newTripContainer, fragment)
            }
        }
    }
}