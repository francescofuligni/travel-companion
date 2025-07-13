

package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R

class StatsFutureFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_future, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val textNextMonth = view.findViewById<TextView>(R.id.textNextMonth)
        val textPredictedTrips = view.findViewById<TextView>(R.id.textPredictedTrips)
        val textEstimatedKm = view.findViewById<TextView>(R.id.textEstimatedKm)
        val imageTrendChart = view.findViewById<ImageView>(R.id.trendChart)
        val textTrend = view.findViewById<TextView>(R.id.textTrend)
        val textSuggestion = view.findViewById<TextView>(R.id.textSuggestion)

        // Valori di esempio (da sostituire con logica dinamica in futuro)
        val nextMonth = "Agosto"
        val predictedTrips = 3
        val estimatedKm = 250
        val isTrendDown = true

        textNextMonth.text = "Prossimo mese: $nextMonth"
        textPredictedTrips.text = "Aspettativa viaggi: #$predictedTrips"
        textEstimatedKm.text = "Stima KM: $estimatedKm km"

        textTrend.text = if (isTrendDown) "Trend: in calo" else "Trend: in crescita"
        textSuggestion.text = if (isTrendDown) "Suggerimento: viaggia di più!" else "Bravo, continua così!"

        // Se hai un'immagine diversa da mostrare a seconda del trend
        imageTrendChart.setImageResource(R.drawable.missing_img)
    }
}