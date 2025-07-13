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

/**
 * Fragment container per la sezione "Nuovo Viaggio"
 * Determina dinamicamente se mostrare la creazione o l'avviso di viaggio attivo
 */
class NewTripFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_new_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadAppropriateFragment()
    }

    /**
     * Carica il fragment appropriato in base allo stato dei viaggi
     * Se c'è un viaggio attivo mostra NewTripActiveFragment, altrimenti NewTripCreateFragment
     */
    private fun loadAppropriateFragment() {
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