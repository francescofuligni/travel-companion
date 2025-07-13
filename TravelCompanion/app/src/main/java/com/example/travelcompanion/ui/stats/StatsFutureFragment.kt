package com.example.travelcompanion.ui.stats

import com.example.travelcompanion.databinding.FragmentStatsFutureBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

import androidx.annotation.RequiresApi
import android.os.Build

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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.travelcompanion.database.TravelDatabase
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

class StatsFutureFragment : Fragment() {
    private var _binding: FragmentStatsFutureBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentStatsFutureBinding.inflate(inflater, container, false)
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadedRed = Color.argb(160, 240, 120, 120)
        val fadedGreen = Color.argb(160, 120, 200, 120)

        // Carica dati storici dal DB e calcola previsioni future
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val trips = withContext(Dispatchers.IO) { db.tripDao().getAllTrips() }
            val now = LocalDate.now()

            // Debug: Log total trips
            android.util.Log.d("StatsFuture", "Total trips found: ${trips.size}")

            val monthsLabels = mutableListOf<String>()
            val tripsDataList = mutableListOf<Float>()
            val kmDataList = mutableListOf<Float>()

            for (i in 4 downTo 0) {
                val month = now.minusMonths(i.toLong())
                monthsLabels.add(month.month.getDisplayName(TextStyle.SHORT, Locale.getDefault()))

                val tripsInMonth = trips.filter { trip ->
                    val tripDate = Instant.ofEpochMilli(trip.startDate)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                    tripDate.year == month.year && tripDate.month == month.month
                }

                tripsDataList.add(tripsInMonth.size.toFloat())
                val distanceSum = tripsInMonth.sumOf { it.distance }.toFloat()
                kmDataList.add(distanceSum)
            }

            if (tripsDataList.isEmpty() || tripsDataList.all { it == 0f }) {
                binding.textNextMonth.text = "Nessun dato disponibile"
                binding.textPredictedTrips.text = "Registra alcuni viaggi per vedere le previsioni"
                binding.textPredictedKm.text = "Registra alcuni viaggi per vedere le previsioni"
            }

            val predictedTrips = if (tripsDataList.isNotEmpty()) {
                (tripsDataList.sum() / tripsDataList.size.toFloat()).roundToInt()
            } else 0

            val predictedKm = if (kmDataList.isNotEmpty()) {
                (kmDataList.sum() / kmDataList.size.toFloat() / 1000).roundToInt()
            } else 0

            val isTrendDownTrips = if (tripsDataList.isNotEmpty()) {
                predictedTrips < tripsDataList.last()
            } else false

            val isTrendDownKm = if (kmDataList.isNotEmpty()) {
                predictedKm < kmDataList.last()
            } else false

            val nextMonthDate = now.plusMonths(1)
            val nextMonthLabel = nextMonthDate.month
                .getDisplayName(TextStyle.FULL, Locale("it", "IT"))
                .replaceFirstChar { it.uppercase(Locale("it", "IT")) }

            binding.textNextMonth.text = "PROSSIMO MESE: $nextMonthLabel"
            binding.textPredictedTrips.text = "Aspettativa numero viaggi: $predictedTrips"
            binding.textPredictedKm.text = "Aspettativa distanza percorsa: $predictedKm km"

            binding.textTrendTripsTitle.text = if (isTrendDownTrips) "Trend: in calo" else "Trend: in crescita"
            binding.textTrendTripsMessage.text = if (isTrendDownTrips)
                "Cerca nuove esperienze e viaggia di più!" else "Continua così, sei un esploratore nato!"
            binding.trendTripsBox.setBackgroundColor(if (isTrendDownTrips) fadedRed else fadedGreen)

            binding.textTrendKmTitle.text = if (isTrendDownKm) "Trend: in calo" else "Trend: in crescita"
            binding.textTrendKmMessage.text = if (isTrendDownKm)
                "Non ti arrendere, guarda oltre i tuoi orizzonti!" else "Continua così, arriverai sempre più lontano!"
            binding.trendKmBox.setBackgroundColor(if (isTrendDownKm) fadedRed else fadedGreen)

            setupChartWithForecast(binding.futureTripsChart, tripsDataList, predictedTrips.toFloat(), "Viaggi", monthsLabels)
            setupChartWithForecast(binding.futureKmChart, kmDataList, predictedKm.toFloat(), "Km percorsi", monthsLabels)
        }
    }

/**
 * Configura un grafico con dati storici e previsioni future
 */
private fun setupChartWithForecast(
    chart: LineChart,
    historicalData: List<Float>,
    predictedValue: Float,
    label: String,
    months: List<String>
) {
    if (historicalData.isEmpty() || months.isEmpty()) {
        android.util.Log.w("StatsFuture", "Empty data for chart: $label")
        return
    }

    val fullData = historicalData + predictedValue
    
    val lastMonthLabel = months.last()
    val allMonthLabels = listOf("Gen","Feb","Mar","Apr","Mag","Giu","Lug","Ago","Set","Ott","Nov","Dic")
    val nextMonth = allMonthLabels[(allMonthLabels.indexOf(lastMonthLabel) + 1) % 12]
    val fullMonths = months + nextMonth

    val entries = fullData.mapIndexed { index, value ->
        Entry(index.toFloat(), value)
    }

    // Segment historical and forecast data
    val historyEntries = entries.dropLast(1)
    val forecastEntryStart = entries[entries.size - 2]
    val forecastEntryEnd = entries.last()

    val historyDataSet = LineDataSet(historyEntries, label).apply {
        color = Color.BLUE
        valueTextColor = Color.BLACK
        setCircleColor(Color.BLUE)
        circleRadius = 4f
        lineWidth = 2f
        setDrawValues(true)
        valueTextSize = 10f
    }

    val forecastColor = if (predictedValue < historicalData.last()) Color.RED else Color.GREEN
    val forecastDataSet = LineDataSet(listOf(forecastEntryStart, forecastEntryEnd), "Previsione").apply {
        color = forecastColor
        valueTextColor = Color.BLACK
        setCircleColor(forecastColor)
        circleRadius = 4f
        lineWidth = 2f
        setDrawValues(true)
        valueTextSize = 10f
        enableDashedLine(10f, 5f, 0f) // Dashed line for forecast
    }

    // Configure chart
    chart.apply {
        data = LineData(historyDataSet, forecastDataSet)
        
        xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(fullMonths)
            granularity = 1f
            position = XAxis.XAxisPosition.BOTTOM
            labelRotationAngle = -45f
            setDrawGridLines(false)
        }
        
        axisLeft.apply {
            granularity = 1f
            axisMinimum = 0f
        }
        
        axisRight.isEnabled = false
        description.isEnabled = false
        legend.isEnabled = true
        
        // Enable touch interactions
        setTouchEnabled(true)
        setScaleEnabled(false)
        setPinchZoom(false)
        
        // Refresh chart
        invalidate()
    }
    
    android.util.Log.d("StatsFuture", "Chart configured for $label with ${entries.size} entries")
}

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}