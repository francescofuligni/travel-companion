package com.example.travelcompanion.ui.stats

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.heatmaps.HeatmapTileProvider
import com.google.android.gms.maps.model.TileOverlayOptions
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import com.example.travelcompanion.database.TravelDatabase

/**
 * Fragment per la visualizzazione della heatmap dei viaggi
 * Mostra una mappa di calore delle posizioni visitate
 */
class StatsHeatmapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats_heatmap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMap()
    }

    /**
     * Configura il fragment della mappa
     */
    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    /**
     * Callback chiamato quando la mappa è pronta
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        configureMapSettings()
        loadHeatmapData()
    }

    /**
     * Configura le impostazioni della mappa
     */
    private fun configureMapSettings() {
        googleMap?.apply {
            uiSettings.isZoomControlsEnabled = true
            uiSettings.isScrollGesturesEnabled = true
            uiSettings.isZoomGesturesEnabled = true
            uiSettings.isRotateGesturesEnabled = true
            uiSettings.isTiltGesturesEnabled = true
            uiSettings.isCompassEnabled = true
            uiSettings.isMapToolbarEnabled = true

            // Imposta un padding per evitare sovrapposizioni
            setPadding(0, 100, 0, 0)

            // Zoom iniziale globale
            moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1.2f))
        }
    }

    /**
     * Carica i dati dal database e genera la heatmap
     */
    private fun loadHeatmapData() {
        lifecycleScope.launch {
            val db = TravelDatabase.getDatabase(requireContext())
            val locations = db.locationDao().getAllLocations()

            if (locations.isNotEmpty()) {
                generateHeatmap(locations)
            }
        }
    }

    /**
     * Genera e visualizza la heatmap dalle posizioni
     */
    private fun generateHeatmap(locations: List<com.example.travelcompanion.database.models.Location>) {
        val latLngs = locations.map { LatLng(it.latitude, it.longitude) }

        val heatmapProvider = HeatmapTileProvider.Builder()
            .data(latLngs)
            .radius(50)
            .build()

        googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(heatmapProvider))
    }
}