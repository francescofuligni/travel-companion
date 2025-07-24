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

/**
 * Fragment per le statistiche future e previsioni
 * Analizza i dati storici per generare previsioni sui viaggi futuri
 */
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
        loadFutureStats()
    }

    /**
     * Carica i dati storici e genera le previsioni future
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun loadFutureStats() {
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val trips = db.tripDao().getAllTrips()
            val now = LocalDate.now()
            val analysisData = analyzeHistoricalData(trips, now)
            
            view?.let { view ->
                updateFutureStatsUI(view, analysisData, now)
            }
        }
    }

    /**
     * Analizza i dati storici degli ultimi 5 mesi
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun analyzeHistoricalData(trips: List<com.example.travelcompanion.database.models.Trip>, now: LocalDate): FutureAnalysisData {
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

        return FutureAnalysisData(monthsLabels, tripsDataList, kmDataList)
    }

    /**
     * Aggiorna l'interfaccia utente con le previsioni future
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateFutureStatsUI(view: View, analysisData: FutureAnalysisData, now: LocalDate) {
        val predictions = calculatePredictions(analysisData)
        
        updateTextViews(view, predictions, now)
        updateTrendBoxes(view, predictions, analysisData)
        updateCharts(view, analysisData, predictions)
    }

    /**
     * Calcola le previsioni basate sui dati storici
     */
    private fun calculatePredictions(analysisData: FutureAnalysisData): PredictionData {
        val predictedTrips = if (analysisData.tripsDataList.isNotEmpty()) {
            (analysisData.tripsDataList.sum() / analysisData.tripsDataList.size.toFloat()).roundToInt()
        } else 0
        
        val predictedKm = if (analysisData.kmDataList.isNotEmpty()) {
            (analysisData.kmDataList.sum() / analysisData.kmDataList.size.toFloat() / 1000).roundToInt()
        } else 0

        return PredictionData(predictedTrips, predictedKm)
    }

    /**
     * Aggiorna i TextView con le previsioni
     */
    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateTextViews(view: View, predictions: PredictionData, now: LocalDate) {
        val textNextMonth = view.findViewById<TextView>(R.id.textNextMonth)
        val textPredictedTrips = view.findViewById<TextView>(R.id.textPredictedTrips)
        val textPredictedKm = view.findViewById<TextView>(R.id.textPredictedKm)

        val nextMonthDate = now.plusMonths(1)
        val nextMonthLabel = nextMonthDate.month
            .getDisplayName(TextStyle.FULL, Locale("it", "IT"))
            .replaceFirstChar { it.uppercase(Locale("it", "IT")) }

        textNextMonth.text = "PROSSIMO MESE: $nextMonthLabel"
        
        if (predictions.predictedTrips == 0 && predictions.predictedKm == 0) {
            textPredictedTrips.text = "Registra alcuni viaggi per vedere le previsioni"
            textPredictedKm.text = "Registra alcuni viaggi per vedere le previsioni"
        } else {
            textPredictedTrips.text = "Aspettativa numero viaggi: ${predictions.predictedTrips}"
            textPredictedKm.text = "Aspettativa distanza percorsa: ${predictions.predictedKm} km"
        }
    }

    /**
     * Aggiorna le box con i trend
     */
    private fun updateTrendBoxes(view: View, predictions: PredictionData, analysisData: FutureAnalysisData) {
        val fadedRed = Color.argb(160, 240, 120, 120)
        val fadedGreen = Color.argb(160, 120, 200, 120)

        val trendTripsBox = view.findViewById<android.widget.LinearLayout>(R.id.trendTripsBox)
        val textTrendTripsTitle = view.findViewById<TextView>(R.id.textTrendTripsTitle)
        val textTrendTripsMessage = view.findViewById<TextView>(R.id.textTrendTripsMessage)
        val trendKmBox = view.findViewById<android.widget.LinearLayout>(R.id.trendKmBox)
        val textTrendKmTitle = view.findViewById<TextView>(R.id.textTrendKmTitle)
        val textTrendKmMessage = view.findViewById<TextView>(R.id.textTrendKmMessage)

        val isTrendDownTrips = if (analysisData.tripsDataList.isNotEmpty()) {
            predictions.predictedTrips < analysisData.tripsDataList.last()
        } else false
        
        val isTrendDownKm = if (analysisData.kmDataList.isNotEmpty()) {
            predictions.predictedKm < analysisData.kmDataList.last()
        } else false

        // Trend viaggi
        textTrendTripsTitle.text = if (isTrendDownTrips) "Trend: in calo" else "Trend: in crescita"
        textTrendTripsMessage.text = if (isTrendDownTrips)
            "Cerca nuove esperienze e viaggia di più!" else "Continua così, sei un esploratore nato!"
        trendTripsBox.setBackgroundColor(if (isTrendDownTrips) fadedRed else fadedGreen)

        // Trend distanza
        textTrendKmTitle.text = if (isTrendDownKm) "Trend: in calo" else "Trend: in crescita"
        textTrendKmMessage.text = if (isTrendDownKm)
            "Non ti arrendere, guarda oltre i tuoi orizzonti!" else "Continua così, arriverai sempre più lontano!"
        trendKmBox.setBackgroundColor(if (isTrendDownKm) fadedRed else fadedGreen)
    }

    /**
     * Aggiorna i grafici con previsioni
     */
    private fun updateCharts(view: View, analysisData: FutureAnalysisData, predictions: PredictionData) {
        val chartTrips = view.findViewById<LineChart>(R.id.futureTripsChart)
        val chartKm = view.findViewById<LineChart>(R.id.futureKmChart)

        setupChartWithForecast(chartTrips, analysisData.tripsDataList, predictions.predictedTrips.toFloat(), "Viaggi", analysisData.monthsLabels)
        setupChartWithForecast(chartKm, analysisData.kmDataList, predictions.predictedKm.toFloat(), "Km percorsi", analysisData.monthsLabels)
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

        // Separa dati storici e previsioni
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
            enableDashedLine(10f, 5f, 0f) // Linea tratteggiata per le previsioni
        }

        // Configura il grafico
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
            
            // Abilita interazioni touch
            setTouchEnabled(true)
            setScaleEnabled(false)
            setPinchZoom(false)
            
            invalidate()
        }
        
        android.util.Log.d("StatsFuture", "Chart configured for $label with ${entries.size} entries")
    }

    /**
     * Classe per contenere i dati di analisi futura
     */
    private data class FutureAnalysisData(
        val monthsLabels: List<String>,
        val tripsDataList: List<Float>,
        val kmDataList: List<Float>
    )

    /**
     * Classe per contenere i dati delle previsioni
     */
    private data class PredictionData(
        val predictedTrips: Int,
        val predictedKm: Int
    )
}