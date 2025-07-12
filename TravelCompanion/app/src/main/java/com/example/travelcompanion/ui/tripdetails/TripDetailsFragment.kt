package com.example.travelcompanion.ui.tripdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentTripDetailsBinding
import com.example.travelcompanion.repository.TravelRepository
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import java.text.SimpleDateFormat
import java.util.*

class TripDetailsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TripDetailsViewModel
    private lateinit var adapter: TripPhasesAdapter
    private var tripId: Long = -1L
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getLong("tripId") ?: -1L
        
        val repository = TravelRepository.create(requireContext())
        val factory = TripDetailsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[TripDetailsViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentTripDetailsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        setupMap()
        setupRecyclerView()
        observeViewModel()
        
        if (tripId != -1L) {
            viewModel.loadTripDetails(tripId)
        }
    }

    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        // Wait for trip data to be loaded to show map markers
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let { updateMapWithTripData(it) }
        }
    }

    private fun updateMapWithTripData(trip: com.example.travelcompanion.database.models.Trip) {
        googleMap?.let { map ->
            map.clear()
            
            // Load trip phases to show route
            viewModel.tripPhases.observe(viewLifecycleOwner) { phases ->
                if (phases.isNotEmpty()) {
                    // Show all phase locations on the map
                    phases.forEach { phase ->
                        // You can add phase locations to the map here if you have location data
                        // For now, we'll show a simple marker for the trip
                    }
                    
                    // Center map on the first phase location or default location
                    val defaultLocation = LatLng(44.0043, 12.6560) // Default location
                    map.addMarker(
                        MarkerOptions()
                            .position(defaultLocation)
                            .title(trip.title)
                            .snippet("Destinazione: ${trip.destination}")
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                }
            }
        }
    }

    private fun setupRecyclerView() {
        adapter = TripPhasesAdapter { phase ->
            // Handle phase click if needed
        }
        binding.rvTripPhases.layoutManager = LinearLayoutManager(context)
        binding.rvTripPhases.adapter = adapter
    }

    private fun observeViewModel() {
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let { 
                Log.d("TripDetailsFragment", "Trip loaded: ${it.title}, ID=${it.id}")
                updateTripHeader(it) 
            }
        }

        viewModel.tripPhases.observe(viewLifecycleOwner) { phases ->
            Log.d("TripDetailsFragment", "Phases loaded: ${phases.size} phases")
            phases.forEach { phase ->
                Log.d("TripDetailsFragment", "Phase ${phase.phaseOrder}: note=${phase.note}, image=${phase.imageUri}")
            }
            adapter.updatePhases(phases)
        }
    }

    private fun formatDuration(durationInSeconds: Long): String {
        return when {
            durationInSeconds < 60 -> "${durationInSeconds}s"
            durationInSeconds < 3600 -> {
                val minutes = durationInSeconds / 60
                val seconds = durationInSeconds % 60
                "${minutes}m ${seconds}s"
            }
            else -> {
                val hours = durationInSeconds / 3600
                val minutes = (durationInSeconds % 3600) / 60
                val seconds = durationInSeconds % 60
                "${hours}h ${minutes}m ${seconds}s"
            }
        }
    }

    private fun updateTripHeader(trip: com.example.travelcompanion.database.models.Trip) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        binding.tvTripTitle.text = trip.title
        binding.tvTripDestination.text = trip.destination
        binding.tvTripStartDate.text = "Inizio: ${dateFormat.format(Date(trip.startDate))}"
        binding.tvTripEndDate.text = if (trip.endDate != 0L) 
            "Fine: ${dateFormat.format(Date(trip.endDate))}" else "In corso"
        
        // Convert distance from meters to kilometers
        val distanceInKm = trip.distance / 1000.0
        binding.tvTripDistance.text = "Distanza: ${String.format("%.2f", distanceInKm)} km"
        
        // Use trip.duration (in seconds) if available, otherwise calculate from dates
        val durationInSeconds = if (trip.duration > 0) {
            trip.duration.toLong()
        } else {
            val endTime = if (trip.endDate != 0L) trip.endDate else System.currentTimeMillis()
            (endTime - trip.startDate) / 1000
        }
        
        binding.tvTripDuration.text = "Durata: ${formatDuration(durationInSeconds)}"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
