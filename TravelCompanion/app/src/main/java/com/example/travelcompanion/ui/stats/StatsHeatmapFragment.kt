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

class StatsHeatmapFragment : Fragment(), OnMapReadyCallback {

    private var googleMap: GoogleMap? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Usa un layout che contiene un SupportMapFragment con id mapView
        return inflater.inflate(R.layout.fragment_stats_heatmap, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapView) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Abilita i controlli di zoom sulla mappa
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isScrollGesturesEnabled = true
        googleMap?.uiSettings?.isZoomGesturesEnabled = true
        googleMap?.uiSettings?.isRotateGesturesEnabled = true
        googleMap?.uiSettings?.isTiltGesturesEnabled = true
        googleMap?.uiSettings?.isCompassEnabled = true
        googleMap?.uiSettings?.isMapToolbarEnabled = true

        // Imposta un padding se necessario per evitare che copra elementi superiori
        googleMap?.setPadding(0, 100, 0, 0)

        // Zoom iniziale più distante per mostrare il mondo
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(0.0, 0.0), 1.2f))

        val dummyLocations = listOf(
            LatLng(48.8566, 2.3522), // Parigi
            LatLng(41.9028, 12.4964), // Roma
            LatLng(40.7128, -74.0060) // New York
        )

        val heatmapProvider = HeatmapTileProvider.Builder()
            .data(dummyLocations)
            .radius(50) // aumenta il raggio per rendere l'area colorata più evidente
            .build()

        googleMap?.addTileOverlay(TileOverlayOptions().tileProvider(heatmapProvider))
    }
}