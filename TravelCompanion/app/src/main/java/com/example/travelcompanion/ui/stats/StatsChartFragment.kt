package com.example.travelcompanion.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
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

        val monthLabels = listOf("Feb", "Mar", "Apr", "Mag", "Giu", "Lug")
        val monthNames = listOf("Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic")
        val nextMonth = monthNames[(monthNames.indexOf(monthLabels.last()) + 1) % 12]
        val tripsData = listOf(2, 3, 1, 4, 2, 3)
        val kmData = listOf(10.5f, 25.0f, 5.8f, 30.2f, 12.0f, 20.0f)

        // Titoli dinamici
        tripsTitle.text = "Viaggi negli ultimi 6 mesi: ${tripsData.sum()}"
        distanceTitle.text = "Distanza percorsa negli ultimi 6 mesi: ${kmData.sum()} km"

        fun setupChart(chart: LineChart, values: List<Float>, labels: List<String>, label: String) {
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

        setupChart(lineChart, tripsData.map { it.toFloat() }, monthLabels, "Viaggi per mese")
        setupChart(distanceChart, kmData, monthLabels, "Km per mese")
    }
}