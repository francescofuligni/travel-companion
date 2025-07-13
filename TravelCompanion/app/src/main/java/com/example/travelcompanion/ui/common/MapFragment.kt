package com.example.travelcompanion.ui.common

import com.google.android.gms.maps.model.LatLng
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R
import com.example.travelcompanion.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Fragment per la visualizzazione di una mappa Google
 * Gestisce la posizione dell'utente e la visualizzazione di marker
 */
class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * Launcher per la richiesta permessi di localizzazione
     */
    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableUserLocation()
        } else {
            Toast.makeText(requireContext(), "Permesso posizione negato", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_map, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as? SupportMapFragment

        mapFragment?.getMapAsync(this)
    }

    /**
     * Callback chiamato quando la mappa è pronta
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap.uiSettings.isZoomControlsEnabled = true
        googleMap.uiSettings.isMyLocationButtonEnabled = true

        if (LocationUtils.hasLocationPermission(requireContext())) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    /**
     * Abilita la localizzazione dell'utente sulla mappa
     */
    private fun enableUserLocation() {
        try {
            googleMap.isMyLocationEnabled = true
            
            LocationUtils.getCurrentLocation(
                requireContext(),
                onSuccess = { currentLatLng ->
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("La tua posizione")
                            .snippet("Posizione corrente")
                    )
                },
                onFailure = { defaultLatLng ->
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
                    googleMap.addMarker(
                        MarkerOptions()
                            .position(defaultLatLng)
                            .title("Posizione di default")
                            .snippet("Impossibile ottenere la posizione corrente")
                    )
                    Toast.makeText(requireContext(), "Impossibile ottenere la posizione corrente", Toast.LENGTH_SHORT).show()
                }
            )
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Mostra una posizione specifica sulla mappa con un marker
     */
    fun showLocation(latLng: LatLng, label: String) {
        if (::googleMap.isInitialized) {
            googleMap.clear()
            googleMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title(label)
            )
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))
        }
    }
}