package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R

/**
 * Fragment per le statistiche dei viaggi storici
 * Permette di navigare tra visualizzazione heatmap e grafici
 */
class StatsMyTrips : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_my_trips, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupViews(view)
        showDefaultFragment()
    }

    /**
     * Configura i pulsanti per la navigazione tra heatmap e grafici
     */
    private fun setupViews(view: View) {
        val btnMap = view.findViewById<MaterialButton>(R.id.btnMap)
        val btnCharts = view.findViewById<MaterialButton>(R.id.btnCharts)

        btnMap.setOnClickListener {
            replaceFragment(StatsHeatmapFragment())
        }

        btnCharts.setOnClickListener {
            replaceFragment(StatsChartFragment())
        }
    }

    /**
     * Mostra il fragment di default (heatmap) all'avvio
     */
    private fun showDefaultFragment() {
        view?.post {
            // Mostra heatmap all'avvio come default, ma solo dopo che la view è stata misurata
            replaceFragment(StatsHeatmapFragment())
        }
    }

    /**
     * Sostituisce il fragment corrente nel container
     */
    private fun replaceFragment(fragment: Fragment) {
        childFragmentManager.beginTransaction()
            .replace(R.id.containerView, fragment)
            .commit()
    }
}