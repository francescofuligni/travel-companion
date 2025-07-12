

package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R
import kotlin.math.roundToInt
import android.graphics.Color
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.components.XAxis

class StatsFutureFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_future, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadedRed = Color.argb(160, 240, 120, 120)   // Rosso chiaro semi-trasparente
        val fadedGreen = Color.argb(160, 120, 200, 120) // Verde chiaro semi-trasparente

        val textNextMonth = view.findViewById<TextView>(R.id.textNextMonth)
        val textPredictedTrips = view.findViewById<TextView>(R.id.textPredictedTrips)
        val textPredictedKm = view.findViewById<TextView>(R.id.textPredictedKm)
        val trendTripsBox = view.findViewById<android.widget.LinearLayout>(R.id.trendTripsBox)
        val textTrendTripsTitle = view.findViewById<TextView>(R.id.textTrendTripsTitle)
        val textTrendTripsMessage = view.findViewById<TextView>(R.id.textTrendTripsMessage)
        val trendKmBox = view.findViewById<android.widget.LinearLayout>(R.id.trendKmBox)
        val textTrendKmTitle = view.findViewById<TextView>(R.id.textTrendKmTitle)
        val textTrendKmMessage = view.findViewById<TextView>(R.id.textTrendKmMessage)

        val chartTrips = view.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.futureTripsChart)
        val chartKm = view.findViewById<com.github.mikephil.charting.charts.LineChart>(R.id.futureKmChart)

        val months = listOf("Mar", "Apr", "Mag", "Giu", "Lug")
        val tripsData = listOf(2, 3, 1, 4, 3)
        val kmData = listOf(120f, 200f, 80f, 250f, 190f)

        val predictedTrips = (tripsData.sum() / tripsData.size.toFloat()).roundToInt()
        val predictedKm = (kmData.sum() / kmData.size.toFloat()).roundToInt()
        val isTrendDownTrips = predictedTrips < tripsData.last()
        val isTrendDownKm = predictedKm < kmData.last()

        val allMonthLabels = listOf("Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic")
        val lastMonth = months.last()
        val nextMonth = allMonthLabels[(allMonthLabels.indexOf(lastMonth) + 1) % 12]
        textNextMonth.text = "PROSSIMO MESE: $nextMonth"
        textPredictedTrips.text = "Aspettativa numero viaggi: $predictedTrips"
        textPredictedKm.text = "Aspettativa distanza percorsa: $predictedKm km"

        textTrendTripsTitle.text = if (isTrendDownTrips) "Trend: in calo" else "Trend: in crescita"
        textTrendTripsMessage.text = if (isTrendDownTrips)
            "Cerca nuove esperienze e viaggia di più!" else "Continua così, sei un esploratore nato!"
        trendTripsBox.setBackgroundColor(if (isTrendDownTrips) fadedRed else fadedGreen)

        textTrendKmTitle.text = if (isTrendDownKm) "Trend: in calo" else "Trend: in crescita"
        textTrendKmMessage.text = if (isTrendDownKm)
            "Non ti arrendere, guarda oltre i tuoi orizzonti!" else "Continua così, arriverai sempre più lontano!"
        trendKmBox.setBackgroundColor(if (isTrendDownKm) fadedRed else fadedGreen)

        fun setupChartWithForecast(
            chart: LineChart,
            historicalData: List<Float>,
            predictedValue: Float,
            label: String,
            months: List<String>
        ) {
            val fullData = historicalData + predictedValue
            val fullMonths = months + nextMonth

            val entries = fullData.mapIndexed { index, value ->
                Entry(index.toFloat(), value)
            }

            val historyEntries = entries.dropLast(1)
            val forecastEntryStart = entries[entries.size - 2]
            val forecastEntryEnd = entries.last()

            val historyDataSet = LineDataSet(historyEntries, label).apply {
                color = Color.BLUE
                valueTextColor = Color.BLACK
                setCircleColor(fadedRed)
                circleRadius = 4f
                lineWidth = 2f
            }

            val forecastColor = if (predictedValue < historicalData.last()) fadedRed else fadedGreen
            val forecastDataSet = LineDataSet(listOf(forecastEntryStart, forecastEntryEnd), "Previsione").apply {
                color = forecastColor
                valueTextColor = Color.BLACK
                setCircleColor(forecastColor)
                circleRadius = 4f
                lineWidth = 2f
            }

            chart.data = LineData(historyDataSet, forecastDataSet)
            chart.xAxis.apply {
                valueFormatter = IndexAxisValueFormatter(fullMonths)
                granularity = 1f
                position = XAxis.XAxisPosition.BOTTOM
                labelRotationAngle = -45f
            }
            chart.axisRight.isEnabled = false
            chart.description.isEnabled = false
            chart.invalidate()
        }

        setupChartWithForecast(chartTrips, tripsData.map { it.toFloat() }, predictedTrips.toFloat(), "Viaggi", months)
        setupChartWithForecast(chartKm, kmData, predictedKm.toFloat(), "Km percorsi", months)
    }
}