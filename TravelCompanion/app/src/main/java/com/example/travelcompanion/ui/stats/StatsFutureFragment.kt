package com.example.travelcompanion.ui.stats

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

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_future, container, false)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fadedRed = Color.argb(160, 240, 120, 120)
        val fadedGreen = Color.argb(160, 120, 200, 120)

        val textNextMonth = view.findViewById<TextView>(R.id.textNextMonth)
        val textPredictedTrips = view.findViewById<TextView>(R.id.textPredictedTrips)
        val textPredictedKm = view.findViewById<TextView>(R.id.textPredictedKm)
        val trendTripsBox = view.findViewById<android.widget.LinearLayout>(R.id.trendTripsBox)
        val textTrendTripsTitle = view.findViewById<TextView>(R.id.textTrendTripsTitle)
        val textTrendTripsMessage = view.findViewById<TextView>(R.id.textTrendTripsMessage)
        val trendKmBox = view.findViewById<android.widget.LinearLayout>(R.id.trendKmBox)
        val textTrendKmTitle = view.findViewById<TextView>(R.id.textTrendKmTitle)
        val textTrendKmMessage = view.findViewById<TextView>(R.id.textTrendKmMessage)

        val chartTrips = view.findViewById<LineChart>(R.id.futureTripsChart)
        val chartKm = view.findViewById<LineChart>(R.id.futureKmChart)

        // Carica dati storici dal DB e calcola previsioni future
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val trips = db.tripDao().getAllTrips()
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
                textNextMonth.text = "Nessun dato disponibile"
                textPredictedTrips.text = "Registra alcuni viaggi per vedere le previsioni"
                textPredictedKm.text = "Registra alcuni viaggi per vedere le previsioni"
            }

            val predictedTrips = if (tripsDataList.isNotEmpty()) {
                (tripsDataList.sum() / tripsDataList.size.toFloat()).roundToInt()
            } else 0
            
            val predictedKm = if (kmDataList.isNotEmpty()) {
                (kmDataList.sum() / kmDataList.size.toFloat()/1000).roundToInt()
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

            textNextMonth.text = "PROSSIMO MESE: $nextMonthLabel"
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

            setupChartWithForecast(chartTrips, tripsDataList, predictedTrips.toFloat(), "Viaggi", monthsLabels)
            setupChartWithForecast(chartKm, kmDataList, predictedKm.toFloat(), "Km percorsi", monthsLabels)
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
}