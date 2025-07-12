package com.example.travelcompanion.ui.tripdetails

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.GridLayoutManager
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentTripDetailsBinding
import com.example.travelcompanion.repository.TravelRepository
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.location.LocationServices
import com.example.travelcompanion.utils.LocationUtils
import android.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

class TripDetailsFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentTripDetailsBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: TripDetailsViewModel
    private lateinit var imagesAdapter: TripImagesAdapter
    private lateinit var notesAdapter: TripNotesAdapter
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
        setupImagesRecyclerView()
        setupNotesRecyclerView()
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
            
            // Load trip phases with locations to show route
            viewModel.tripPhasesWithLocations.observe(viewLifecycleOwner) { phasesWithLocations ->
                if (phasesWithLocations.isNotEmpty()) {
                    val boundsBuilder = LatLngBounds.Builder()
                    val routePoints = mutableListOf<LatLng>()
                    
                    // Add markers for each phase location
                    phasesWithLocations.forEachIndexed { index, phaseWithLocation ->
                        val location = phaseWithLocation.location
                        val phase = phaseWithLocation.phase
                        val latLng = LatLng(location.latitude, location.longitude)
                        
                        // Add marker
                        val markerOptions = MarkerOptions()
                            .position(latLng)
                            .title("Fase ${phase.phaseOrder}")
                            .snippet("${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(phase.timestamp))}")
                        
                        // Use different colors for start, middle, and end points
                        when {
                            index == 0 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                            index == phasesWithLocations.size - 1 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                            else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
                        }
                        
                        map.addMarker(markerOptions)
                        routePoints.add(latLng)
                        boundsBuilder.include(latLng)
                    }
                    
                    // Add polyline to connect all points
                    if (routePoints.size > 1) {
                        val polylineOptions = PolylineOptions()
                            .addAll(routePoints)
                            .color(Color.BLUE)
                            .width(5f)
                        map.addPolyline(polylineOptions)
                    }
                    
                    // Adjust camera to show all markers
                    try {
                        val bounds = boundsBuilder.build()
                        val padding = 100 // padding in pixels
                        map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                    } catch (e: Exception) {
                        // If bounds building fails, center on first location
                        val firstLocation = routePoints.first()
                        map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15f))
                    }
                } else {
                    // No phases found, try to use GPS location or show trip destination
                    LocationUtils.getCurrentLocation(
                        requireContext(),
                        onSuccess = { currentLatLng ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title(trip.title)
                                    .snippet("Posizione corrente - Destinazione: ${trip.destination}")
                            )
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        },
                        onFailure = { defaultLatLng ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(defaultLatLng)
                                    .title(trip.title)
                                    .snippet("Destinazione: ${trip.destination}")
                            )
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
                        }
                    )
                }
            }
        }
    }

    private fun setupImagesRecyclerView() {
        imagesAdapter = TripImagesAdapter { image ->
            // TODO: Handle image click - could open full screen image viewer
            Log.d("TripDetailsFragment", "Image clicked: ${image.uri}")
        }
        binding.rvTripImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTripImages.adapter = imagesAdapter
    }

    private fun setupNotesRecyclerView() {
        notesAdapter = TripNotesAdapter { note ->
            // TODO: Handle note click - could open note editor
            Log.d("TripDetailsFragment", "Note clicked: ${note.content}")
        }
        binding.rvTripNotes.layoutManager = LinearLayoutManager(context)
        binding.rvTripNotes.adapter = notesAdapter
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
            // Phases are now only used for the map display
        }

        viewModel.tripNotes.observe(viewLifecycleOwner) { notes ->
            Log.d("TripDetailsFragment", "Notes loaded: ${notes.size} notes")
            updateNotesSection(notes)
        }

        viewModel.tripImages.observe(viewLifecycleOwner) { images ->
            Log.d("TripDetailsFragment", "Images loaded: ${images.size} images")
            updateImagesSection(images)
        }
    }

    private fun updateNotesSection(notes: List<com.example.travelcompanion.database.models.Note>) {
        if (notes.isEmpty()) {
            binding.tvNoNotes.visibility = View.VISIBLE
            binding.rvTripNotes.visibility = View.GONE
        } else {
            binding.tvNoNotes.visibility = View.GONE
            binding.rvTripNotes.visibility = View.VISIBLE
            notesAdapter.updateNotes(notes)
        }
    }

    private fun updateImagesSection(images: List<com.example.travelcompanion.database.models.Image>) {
        if (images.isEmpty()) {
            binding.tvNoImages.visibility = View.VISIBLE
            binding.rvTripImages.visibility = View.GONE
        } else {
            binding.tvNoImages.visibility = View.GONE
            binding.rvTripImages.visibility = View.VISIBLE
            imagesAdapter.updateImages(images)
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
