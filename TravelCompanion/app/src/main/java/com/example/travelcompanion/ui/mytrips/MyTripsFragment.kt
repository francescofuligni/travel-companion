package com.example.travelcompanion.ui.mytrips

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import java.util.Calendar
import com.example.travelcompanion.ui.mytrips.YearPickerDialog
import androidx.core.content.ContextCompat
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentMyTripsBinding
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository

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

        val repository = TravelRepository.create(requireContext())
        val factory = MyTripsVMFactory(repository)
        viewModel = ViewModelProvider(this, factory)[MyTripsViewModel::class.java]

        adapter = MyTripsAdapter(emptyList())
        binding.rvMyTrips.adapter = adapter

        viewModel.trips.observe(viewLifecycleOwner) { trips ->
            adapter.submitList(trips.map {
                TripUiModel(
                    title = it.title,
                    destination = it.destination,
                    imageUrl = null // eventualmente sostituibile con un campo reale
                )
            })
            toggleEmptyView()
        }

        // Divider tra gli elementi della lista
        binding.rvMyTrips.addItemDecoration(
            DividerItemDecoration(requireContext(), DividerItemDecoration.VERTICAL)
        )
        // Mostra messaggio se lista vuota
        toggleEmptyView()

        // Apri dialog per selezione anno
        binding.filterButton.setOnClickListener {
            val currentYear = Calendar.getInstance().get(Calendar.YEAR)
            YearPickerDialog(
                minYear = currentYear - 150,
                maxYear = currentYear
            ) { year ->
                binding.filterButton.text = year.toString()
                binding.ivClearFilter.visibility = View.VISIBLE
                onYearPicked(year)
            }.show(childFragmentManager, "yearPicker")
        }

        // Reset del filtro
        binding.ivClearFilter.setOnClickListener {
            binding.filterButton.text = "Filtra per anno"
            binding.ivClearFilter.visibility = View.GONE
            onFilterReset()
        }
    }

    private fun onYearPicked(year: Int) {
        viewModel.filterTripsByYear(year)
    }

    private fun onFilterReset() {
        viewModel.resetFilter()
    }

    private fun toggleEmptyView() {
        binding.tvEmptyState.visibility = if ((binding.rvMyTrips.adapter?.itemCount ?: 0) == 0) {
            View.VISIBLE
        } else {
            View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}