package com.example.travelcompanion.ui.stats

import com.example.travelcompanion.databinding.FragmentStatsMyTripsBinding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R

class StatsMyTrips : Fragment() {
    private var _binding: FragmentStatsMyTripsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatsMyTripsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.containerView.post {
            // Mostra heatmap all'avvio come default, ma solo dopo che la view è stata misurata
            replaceFragment(StatsHeatmapFragment())
        }

        binding.btnMap.setOnClickListener {
            replaceFragment(StatsHeatmapFragment())
        }

        binding.btnCharts.setOnClickListener {
            replaceFragment(StatsChartFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerView, fragment)
            .commit()
    }
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}