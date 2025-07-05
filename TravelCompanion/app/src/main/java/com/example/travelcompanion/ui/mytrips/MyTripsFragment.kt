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

class MyTripsFragment : Fragment() {
    private var _binding: FragmentMyTripsBinding? = null
    private val binding get() = _binding!!

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
        // TODO: implementa il filtro dei viaggi per l'anno selezionato
    }

    private fun onFilterReset() {
        // TODO: implementa la logica di reset del filtro dei viaggi
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}