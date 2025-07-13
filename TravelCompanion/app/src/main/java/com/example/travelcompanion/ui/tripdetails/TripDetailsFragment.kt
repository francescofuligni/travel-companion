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
import com.example.travelcompanion.database.models.Note
import com.example.travelcompanion.database.models.Trip
import java.text.SimpleDateFormat
import java.util.*

/**
 * Fragment per visualizzare i dettagli completi di un viaggio
 * Include mappa con percorso, immagini, note e statistiche del viaggio
 */
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
        
        setupComponents()
        
        if (tripId != -1L) {
            viewModel.loadTripDetails(tripId)
        }
    }

    /**
     * Configura tutti i componenti del fragment
     */
    private fun setupComponents() {
        setupMap()
        setupImagesRecyclerView()
        setupNotesRecyclerView()
        observeViewModel()
    }

    /**
     * Configura il fragment della mappa
     */
    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.mapFragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    /**
     * Callback chiamato quando la mappa è pronta
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        
        // Attende che i dati del viaggio siano caricati per mostrare i marker
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let { updateMapWithTripData(it) }
        }
    }

    /**
     * Aggiorna la mappa con i dati del viaggio e il percorso
     */
    private fun updateMapWithTripData(trip: Trip) {
        googleMap?.let { map ->
            map.clear()
            
            // Carica le fasi del viaggio con le posizioni per mostrare il percorso
            viewModel.tripPhasesWithLocations.observe(viewLifecycleOwner) { phasesWithLocations ->
                if (phasesWithLocations.isNotEmpty()) {
                    displayTripRoute(map, phasesWithLocations)
                } else {
                    displayFallbackLocation(map, trip)
                }
            }
        }
    }

    /**
     * Visualizza il percorso completo del viaggio con marker e polyline
     */
    private fun displayTripRoute(map: GoogleMap, phasesWithLocations: List<com.example.travelcompanion.database.models.TripPhaseWithLocation>) {
        val boundsBuilder = LatLngBounds.Builder()
        val routePoints = mutableListOf<LatLng>()
        
        // Aggiunge marker per ogni fase del viaggio
        phasesWithLocations.forEachIndexed { index, phaseWithLocation ->
            val location = phaseWithLocation.location
            val phase = phaseWithLocation.phase
            val latLng = LatLng(location.latitude, location.longitude)
            
            // Crea marker con informazioni della fase
            val markerOptions = MarkerOptions()
                .position(latLng)
                .title("Fase ${phase.phaseOrder}")
                .snippet(SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(phase.timestamp)))
            
            // Usa colori diversi per punto di partenza, intermedi e arrivo
            when (index) {
                0 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN))
                phasesWithLocations.size - 1 -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
                else -> markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_BLUE))
            }
            
            map.addMarker(markerOptions)
            routePoints.add(latLng)
            boundsBuilder.include(latLng)
        }
        
        // Aggiunge polyline per collegare tutti i punti
        if (routePoints.size > 1) {
            val polylineOptions = PolylineOptions()
                .addAll(routePoints)
                .color(Color.BLUE)
                .width(5f)
            map.addPolyline(polylineOptions)
        }
        
        // Regola la camera per mostrare tutti i marker
        adjustCameraToShowAllMarkers(map, boundsBuilder, routePoints)
    }

    /**
     * Regola la camera per mostrare tutti i marker del percorso
     */
    private fun adjustCameraToShowAllMarkers(map: GoogleMap, boundsBuilder: LatLngBounds.Builder, routePoints: List<LatLng>) {
        try {
            val bounds = boundsBuilder.build()
            val padding = 100 // padding in pixels
            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
        } catch (e: Exception) {
            // Se la creazione dei bounds fallisce, centra sulla prima posizione
            val firstLocation = routePoints.first()
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(firstLocation, 15f))
        }
    }

    /**
     * Mostra una posizione di fallback quando non ci sono fasi del viaggio
     */
    private fun displayFallbackLocation(map: GoogleMap, trip: Trip) {
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

    /**
     * Configura la RecyclerView per le immagini del viaggio
     */
    private fun setupImagesRecyclerView() {

        binding.rvTripImages.layoutManager = LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        binding.rvTripImages.adapter = imagesAdapter
    }

    /**
     * Configura la RecyclerView per le note del viaggio
     */
    private fun setupNotesRecyclerView() {

        binding.rvTripNotes.layoutManager = LinearLayoutManager(context)
        binding.rvTripNotes.adapter = notesAdapter
    }

    /**
     * Configura gli observer per i dati del ViewModel
     */
    private fun observeViewModel() {
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let { 
                Log.d("TripDetailsFragment", "Trip loaded: ${it.title}, ID=${it.id}")
                updateTripHeader(it) 
            }
        }

        viewModel.tripPhases.observe(viewLifecycleOwner) { phases ->
            Log.d("TripDetailsFragment", "Phases loaded: ${phases.size} phases")
            // Le fasi sono ora usate solo per la visualizzazione sulla mappa
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

    /**
     * Aggiorna la sezione delle note mostrando/nascondendo la lista
     */
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

    /**
     * Aggiorna la sezione delle immagini mostrando/nascondendo la lista
     */
    private fun updateImagesSection(images: List<Image>) {
        if (images.isEmpty()) {
            binding.tvNoImages.visibility = View.VISIBLE
            binding.rvTripImages.visibility = View.GONE
        } else {
            binding.tvNoImages.visibility = View.GONE
            binding.rvTripImages.visibility = View.VISIBLE
            imagesAdapter.updateImages(images)
        }
    }

    /**
     * Formatta la durata in secondi in formato leggibile
     */
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

    /**
     * Aggiorna l'header con le informazioni principali del viaggio
     */
    private fun updateTripHeader(trip: com.example.travelcompanion.database.models.Trip) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        
        binding.tvTripTitle.text = trip.title
        binding.tvTripDestination.text = trip.destination
        binding.tvTripStartDate.text = "Inizio: ${dateFormat.format(Date(trip.startDate))}"
        binding.tvTripEndDate.text = if (trip.endDate != 0L) 
            "Fine: ${dateFormat.format(Date(trip.endDate))}" else "In corso"
        
        // Converte la distanza da metri a chilometri
        val distanceInKm = trip.distance / 1000.0
        binding.tvTripDistance.text = "Distanza: ${String.format("%.2f", distanceInKm)} km"
        
        // Usa trip.duration (in secondi) se disponibile, altrimenti calcola dalle date
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
