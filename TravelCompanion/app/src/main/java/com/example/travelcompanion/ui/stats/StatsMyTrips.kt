package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.core.content.ContextCompat
import android.content.res.ColorStateList
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R

class StatsMyTrips : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_my_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val btnMap = view.findViewById<MaterialButton>(R.id.btnMap)
        val btnCharts = view.findViewById<MaterialButton>(R.id.btnCharts)

        // Stile iniziale: map selezionata, charts non selezionato
        updateButtonStyles(btnMap, btnCharts)

        view.post {
            // Mostra heatmap all'avvio come default, ma solo dopo che la view è stata misurata
            replaceFragment(StatsHeatmapFragment())
        }

        btnMap.setOnClickListener {
            updateButtonStyles(btnMap, btnCharts)
            replaceFragment(StatsHeatmapFragment())
        }

        btnCharts.setOnClickListener {
            updateButtonStyles(btnCharts, btnMap)
            replaceFragment(StatsChartFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerView, fragment)
            .commit()
    }

    private fun updateButtonStyles(selected: MaterialButton, unselected: MaterialButton) {
        // Filled per il bottone selezionato
        selected.strokeWidth = 0
        selected.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        selected.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        // Outlined per il bottone non selezionato
        val strokeWidthPx = (2 * resources.displayMetrics.density).toInt()
        unselected.strokeWidth = strokeWidthPx
        unselected.strokeColor = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), R.color.colorPrimary)
        )
        unselected.backgroundTintList = ColorStateList.valueOf(
            ContextCompat.getColor(requireContext(), android.R.color.transparent)
        )
        unselected.setTextColor(ContextCompat.getColor(requireContext(), R.color.colorPrimary))
    }
}