package com.example.travelcompanion.ui.mytrips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.util.Calendar
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentMyTripsBinding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripType
import com.example.travelcompanion.repository.TravelRepository

/**
 * Fragment per visualizzare e filtrare la lista dei viaggi dell'utente
 * Permette filtri per anno e tipo di viaggio
 */
class MyTripsFragment : Fragment() {
    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: MyTripsViewModel
    private lateinit var adapter: MyTripsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMyTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupViewModel()
        setupRecyclerView()
        setupObservers()
        setupFilters()
    }

    /**
     * Configura il ViewModel con repository injection
     */
    private fun setupViewModel() {
        val repository = TravelRepository.create(requireContext())
        val factory = MyTripsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MyTripsViewModel::class.java]
    }

    /**
     * Configura la RecyclerView con adapter e decorazioni
     */
    private fun setupRecyclerView() {
        adapter = MyTripsAdapter(emptyList()) { tripId ->
            // Navigate to trip details
            navigateToTripDetails(tripId)
        }
        binding.rvMyTrips.adapter = adapter
        binding.rvMyTrips.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
    }

    /**
     * Configura gli observer per i dati del ViewModel
     */
    private fun setupObservers() {
        // Osserva i viaggi filtrati e aggiorna la lista
        viewModel.tripUiModels.observe(viewLifecycleOwner) { tripUiModels ->
            adapter.submitList(tripUiModels)
            toggleEmptyView()
        }
    }

    /**
     * Configura i filtri per anno e tipo di viaggio
     */
    private fun setupFilters() {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        
        // Imposta filtro anno corrente di default
        binding.filterButton.text = currentYear.toString()
        viewModel.setYear(currentYear)

        // Bottone selezione anno
        binding.filterButton.setOnClickListener {
            YearPickerDialog(
                minYear = currentYear - 150,
                maxYear = currentYear
            ) { year ->
                binding.filterButton.text = year.toString()
                binding.ivClearFilter.visibility = View.VISIBLE
                viewModel.setYear(year)
            }.show(childFragmentManager, "yearPicker")
        }

        // Filtro tipo viaggio (MaterialButtonToggleGroup)
        binding.toggleTripType.addOnButtonCheckedListener { group, checkedId, isChecked ->
            if (isChecked) {
                val type = when (checkedId) {
                    R.id.btnLocal -> TripType.LOCAL
                    R.id.btnOneDay -> TripType.ONE_DAY
                    R.id.btnMultiDays -> TripType.MULTI_DAYS
                    else -> null
                }
                viewModel.setType(type)
            } else if (group.checkedButtonId == View.NO_ID) {
                // Nessun bottone selezionato - rimuovi filtro tipo
                viewModel.setType(null)
            }
        }

        // Bottone cancella filtri
        binding.ivClearFilter.setOnClickListener {
            clearAllFilters()
        }
    }

    /**
     * Cancella tutti i filtri attivi e ripristina lo stato iniziale
     */
    private fun clearAllFilters() {
        binding.filterButton.text = "Filtra per anno"
        binding.ivClearFilter.visibility = View.GONE
        binding.toggleTripType.clearChecked()
        viewModel.resetFilters()
    }

    /**
     * Mostra/nasconde la vista vuota in base al numero di elementi
     */
    private fun toggleEmptyView() {
        val isEmpty = (binding.rvMyTrips.adapter?.itemCount ?: 0) == 0
        binding.tvEmptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /**
     * Extension function per convertire Trip in TripUiModel
     * Separa la logica di presentazione dal modello dati
     */
    private fun Trip.toUiModel(imageUrl: String? = null): TripUiModel {
        return TripUiModel(
            id = this.id,
            title = this.title,
            destination = this.destination,
            imageUrl = imageUrl,
            startDate = this.startDate,
            endDate = this.endDate,
            type = this.type,
            distance = this.distance,
            duration = this.duration,
        )
    }

    /**
     * Naviga ai dettagli del viaggio
     */
    private fun navigateToTripDetails(tripId: Long) {
        val bundle = Bundle().apply {
            putLong("tripId", tripId)
        }
        findNavController().navigate(R.id.action_nav_my_trips_to_nav_trip_details, bundle)
    }
}