package com.example.travelcompanion.ui.home

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.travelcompanion.R
import com.example.travelcompanion.utils.LocationUtils
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

class MapFragment : Fragment(), OnMapReadyCallback {

    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    // Gestione richiesta permessi runtime
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

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (LocationUtils.hasLocationPermission(requireContext())) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private fun enableUserLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
            
            LocationUtils.getCurrentLocation(
                requireContext(),
                onSuccess = { currentLatLng ->
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                    // Add a marker at the current location
                    googleMap?.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .title("La tua posizione")
                            .snippet("Posizione corrente")
                    )
                },
                onFailure = { defaultLatLng ->
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
                    googleMap?.addMarker(
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
}