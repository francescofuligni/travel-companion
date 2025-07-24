package com.example.travelcompanion.ui.stats

import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
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

/**
 * Fragment per la visualizzazione dei grafici storici
 * Mostra grafici a linea per viaggi e distanze negli ultimi 6 mesi
 */
class StatsChartFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_chart, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        loadChartData()
    }

    /**
     * Carica i dati dal database e popola i grafici
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadChartData() {
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val trips = db.tripDao().getAllTrips()
            val chartData = generateChartData(trips)
            
            view?.let { view ->
                updateChartsAndTitles(view, chartData)
            }
        }
    }

    /**
     * Genera i dati per i grafici dagli ultimi 6 mesi
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun generateChartData(trips: List<com.example.travelcompanion.database.models.Trip>): ChartData {
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

            // Somma le distanze convertite in km
            val distanceSum = tripsInMonth.sumOf { it.distance }.toFloat() / 1000
            kmData.add(distanceSum)
        }

        return ChartData(monthLabels, tripsData, kmData)
    }

    /**
     * Aggiorna i grafici e i titoli con i dati generati
     */
    private fun updateChartsAndTitles(view: View, chartData: ChartData) {
        val lineChart = view.findViewById<LineChart>(R.id.lineChart)
        val distanceChart = view.findViewById<LineChart>(R.id.distanceChart)
        val tripsTitle = view.findViewById<TextView>(R.id.tripsTitle)
        val distanceTitle = view.findViewById<TextView>(R.id.distanceTitle)

        // Aggiorna i titoli con i totali
        tripsTitle.text = "Viaggi negli ultimi 6 mesi: ${chartData.tripsData.sum().toInt()}"
        distanceTitle.text = "Distanza percorsa negli ultimi 6 mesi: ${chartData.kmData.sum().toInt()} km"

        // Configura i grafici
        setupChart(lineChart, chartData.tripsData, chartData.monthLabels, "Viaggi per mese")
        setupChart(distanceChart, chartData.kmData, chartData.monthLabels, "Km per mese")
    }

    /**
     * Configura un grafico a linea con i dati forniti
     */
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
            setDrawValues(true)
            valueTextSize = 10f
        }

        chart.apply {
            data = LineData(dataSet)
            xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(labels)
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
                labelRotationAngle = -45f
            }
            
            axisRight.isEnabled = false
            description.isEnabled = false
            
            invalidate()
        }
    }

    /**
     * Classe per contenere i dati dei grafici
     */
    private data class ChartData(
        val monthLabels: List<String>,
        val tripsData: List<Float>,
        val kmData: List<Float>
    )
}