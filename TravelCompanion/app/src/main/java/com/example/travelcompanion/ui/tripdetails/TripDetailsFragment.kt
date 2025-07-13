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
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLngBounds
import com.example.travelcompanion.utils.LocationUtils
import android.graphics.Color
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.Note
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripPhase
import androidx.lifecycle.Observer
import com.example.travelcompanion.database.models.TripPhaseWithLocation
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

    private val dateFormatter by lazy {
        SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
    }

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
        observeMapData()
        
        if (tripId != -1L) {
            viewModel.loadTripDetails(tripId)
        }
    }

    private fun observeMapData() {
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let { updateMapWithTripData(it) }
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
    }

    private fun drawMarkers(map: GoogleMap, phasesWithLocations: List<Pair<TripPhase, Location>>) {
        val boundsBuilder = LatLngBounds.Builder()
        val routePoints = mutableListOf<LatLng>()
        phasesWithLocations.forEachIndexed { index, (phase, location) ->
            val latLng = LatLng(location.latitude, location.longitude)
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Fase ${phase.phaseOrder}")
                .snippet(dateFormatter.format(Date(phase.timestamp)))
            when (index) {
                0 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                phasesWithLocations.size - 1 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            map.addMarker(markerOptions)
            routePoints.add(latLng)
            boundsBuilder.include(latLng)
        }
        drawPolyline(map, routePoints)
        try {
            val bounds = boundsBuilder.build()
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 100))
        } catch (e: Exception) {
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(routePoints.first(), 15f))
        }
    }
    
    private fun drawPolyline(map: GoogleMap, points: List<LatLng>) {
        if (points.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(points)
                .color(Color.BLUE)
                .width(5f)
            map.addPolyline(polylineOptions)
        }
    }

    private fun updateMapWithTripData(trip: Trip) {
        val map = googleMap ?: return

        // Creo un observer che si auto‐rimuove alla prima invocazione
        val phasesObserver = object : Observer<List<TripPhaseWithLocation>> {
            override fun onChanged(phasesWithLocations: List<TripPhaseWithLocation>) {
                // Rimuovo l’observer prima di fare qualsiasi operazione, così gira solo una volta
                viewModel.tripPhasesWithLocations.removeObserver(this)

                if (phasesWithLocations.isNotEmpty()) {
                    drawMarkers(map, phasesWithLocations.map { it.phase to it.location })
                } else {
                    // fallback esistente…
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

        viewModel.tripPhasesWithLocations.observe(viewLifecycleOwner, phasesObserver)
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

    private fun updateNotesSection(notes: List<Note>) {
        if (notes.isEmpty()) {
            binding.tvNoNotes.visibility = View.VISIBLE
            binding.rvTripNotes.visibility = View.GONE
        } else {
            binding.tvNoNotes.visibility = View.GONE
            binding.rvTripNotes.visibility = View.VISIBLE
            notesAdapter.updateNotes(notes)
        }
    }

    private fun updateImagesSection(images: List<Image>) {
        if (images.isEmpty()) {
            binding.tvNoImages.visibility = View.VISIBLE
            binding.rvTripImages.visibility = View.GONE
        } else {
            binding.tvNoImages.visibility = View.GONE
            binding.rvTripImages.visibility = View.VISIBLE
            imagesAdapter.submitList(images)
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
        binding.tvTripTitle.text = trip.title
        binding.tvTripDestination.text = trip.destination
        binding.tvTripStartDate.text = "Inizio: ${dateFormatter.format(Date(trip.startDate))}"
        binding.tvTripEndDate.text = if (trip.endDate != 0L) 
            "Fine: ${dateFormatter.format(Date(trip.endDate))}" else "In corso"
        
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
