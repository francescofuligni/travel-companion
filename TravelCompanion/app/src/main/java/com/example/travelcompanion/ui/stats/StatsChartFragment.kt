package com.example.travelcompanion.ui.stats

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
        val lineChart = view.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.lineChart)

        // Dati fittizi per gli ultimi 12 mesi
        val labels = listOf(
            "Ago", "Set", "Ott", "Nov", "Dic", "Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug"
        )
        val values = listOf(2, 1, 3, 0, 4, 2, 1, 3, 2, 4, 3, 5)

        val entries = values.mapIndexed { index, value ->
            com.github.mikephil.charting.data.Entry(index.toFloat(), value.toFloat())
        }

        val dataSet = com.github.mikephil.charting.data.LineDataSet(entries, "Viaggi per mese")
        dataSet.color = Color.BLUE
        dataSet.valueTextColor = Color.BLACK
        dataSet.setCircleColor(Color.RED)
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 2f

        val lineData = com.github.mikephil.charting.data.LineData(dataSet)
        lineChart.data = lineData

        val xAxis = lineChart.xAxis
        xAxis.valueFormatter = com.github.mikephil.charting.formatter.IndexAxisValueFormatter(labels)
        xAxis.granularity = 1f
        xAxis.position = com.github.mikephil.charting.components.XAxis.XAxisPosition.BOTTOM
        xAxis.labelRotationAngle = -45f

        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }
}