package com.example.travelcompanion.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.travelcompanion.database.TravelDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale
import com.example.travelcompanion.R
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis

class StatsChartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)
        val distanceChart = view.findViewById<LineChart>(R.id.distanceChart)

        val tripsTitle = view.findViewById<TextView>(R.id.tripsTitle)
        val distanceTitle = view.findViewById<TextView>(R.id.distanceTitle)

        // Popola i dati del grafico dagli ultimi 6 mesi
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val trips = db.tripDao().getAllTrips()

            val monthLabels = mutableListOf<String>()
            val tripsData = mutableListOf<Float>()
            val kmData = mutableListOf<Float>()
            val now = LocalDate.now()

            for (i in 5 downTo 0) {
                val month = now.minusMonths(i.toLong())
                monthLabels.add(month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))

                // Filtra i viaggi iniziati in questo mese
                val tripsInMonth = trips.filter { trip ->
                    val tripDate = Instant.ofEpochMilli(trip.startDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    tripDate.year == month.year && tripDate.month == month.month
                }
                tripsData.add(tripsInMonth.size.toFloat())

                // Somma le distanze (assumendo campo `distance` su Trip)
                val distanceSum = tripsInMonth.sumOf { it.distance }.toFloat()
                kmData.add(distanceSum)
            }

            // Aggiorna titoli e grafici
            tripsTitle.text = "Viaggi negli ultimi 6 mesi: ${tripsData.sum()}"
            distanceTitle.text = "Distanza percorsa negli ultimi 6 mesi: ${kmData.sum()} km"
            setupChart(lineChart, tripsData, monthLabels, "Viaggi per mese")
            setupChart(distanceChart, kmData, monthLabels, "Km per mese")
        }
    }

    private fun setupChart(chart: LineChart, values: List<Float>, labels: List<String>, label: String) {
        val entries = values.mapIndexed { index, value ->
            Entry(index.toFloat(), value)
        }

        val dataSet = LineDataSet(entries, label).apply {
            color = Color.BLUE
            valueTextColor = Color.BLACK
            setCircleColor(Color.RED)
            circleRadius = 4f
            lineWidth = 2f
        }

        chart.data = LineData(dataSet)
        chart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(labels)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            labelRotationAngle = -45f
        }
        chart.axisRight.isEnabled = false
        chart.description.isEnabled = false
        chart.invalidate()
    }
}