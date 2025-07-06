package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
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

        view.post {
            // Mostra heatmap all'avvio come default, ma solo dopo che la view è stata misurata
            replaceFragment(StatsHeatmapFragment())
        }

        btnMap.setOnClickListener {
            replaceFragment(StatsHeatmapFragment())
        }

        btnCharts.setOnClickListener {
            replaceFragment(StatsChartFragment())
        }
    }

    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerView, fragment)
            .commit()
    }
}