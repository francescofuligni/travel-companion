package com.example.travelcompanion.ui.home

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.R
import android.widget.TextView
import android.widget.Button
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.*
import android.util.Log
import com.example.travelcompanion.ui.common.AddNoteDialogFragment
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.LocationServices
import com.example.travelcompanion.utils.LocationUtils

import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.model.Polyline
import com.google.android.gms.maps.model.PolylineOptions
import android.os.Looper
import android.graphics.Color

import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.core.app.ActivityCompat

/**
 * Fragment che mostra i dettagli di un viaggio attivo in corso
 * Include una mappa per visualizzare il percorso in tempo reale
 */
class HomeActiveTripFragment : Fragment(), OnMapReadyCallback {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var userPath: Polyline? = null
    private val userPathPoints = mutableListOf<LatLng>()

    private var tripId: Long = -1L
    private lateinit var viewModel: HomeActiveTripViewModel
    private lateinit var chronometer: Chronometer
    private var isChronoRunning = false
    private var googleMap: GoogleMap? = null

    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            takePicture()
        } else {
            Toast.makeText(requireContext(), "Permesso fotocamera negato", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(
        ActivityResultContracts.TakePicture()
    ) { success ->
        if (success) {
            imageUri?.let { uri ->
                saveImageToCurrentPhase(uri)
            }
        }
    }

    private var imageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getLong("tripId") ?: -1L
        Log.d("HomeActiveTripFragment", "Trip ID: $tripId")
        
        val repository = TravelRepository.create(requireContext())
        val factory = HomeActiveTripViewModelFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[HomeActiveTripViewModel::class.java]

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_active_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        if (tripId == -1L) {
            Log.e("HomeActiveTripFragment", "Trip ID non valido")
            return
        }
        
        setupViews(view)
        setupMap()
        viewLifecycleOwner.lifecycleScope.launch {
            observeTrip()
        }

        // Setup location callback for real-time updates
        setupLocationCallback()
    }
    
    /**
     * Configura le view e i listener
     */
    private fun setupViews(view: View) {
        // Initialize views
        val tvTripTitle = view.findViewById<TextView>(R.id.tvHomeTripTitle)
        val tvStartDate = view.findViewById<TextView>(R.id.tv_start_date)
        val tvEndDate = view.findViewById<TextView>(R.id.tv_end_date)
        val tvDistance = view.findViewById<TextView>(R.id.tv_distance)
        chronometer = view.findViewById(R.id.chronometer)
        
        // Bottoni
        val stopButton = view.findViewById<Button>(R.id.btn_stop)
        val photoButton = view.findViewById<Button>(R.id.btn_photo)
        val noteButton = view.findViewById<Button>(R.id.btn_note)
        
        // Stop trip button
        stopButton.setOnClickListener {
            Log.d("HomeActiveTripFragment", "Stop button clicked")
            stopTrip()
        }
        
        // Photo button
        photoButton.setOnClickListener {
            Log.d("HomeActiveTripFragment", "Photo button clicked")
            checkCameraPermissionAndTakePicture()
        }
        
        // Note button
        noteButton.setOnClickListener {
            Log.d("HomeActiveTripFragment", "Note button clicked")
            showAddNoteDialog()
        }
    }
    
    /**
     * Configura la mappa integrata nel fragment
     */
    private fun setupMap() {
        val mapFragment = childFragmentManager
            .findFragmentById(R.id.map_fragment) as? SupportMapFragment
        mapFragment?.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true
        
        // Configura la mappa per il viaggio attivo
        setupActiveTripMap()

        // Start location updates if permission granted
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
    }

    /**
     * Configura la mappa per mostrare il percorso del viaggio attivo
     */
    private fun setupActiveTripMap() {
        // Correzione: usa viewModel.trip invece di viewModel.currentTrip
        viewModel.trip.observe(viewLifecycleOwner) { trip ->
            trip?.let {
                updateMapWithTripData(it)
            }
        }
    }

    /**
     * Aggiorna la mappa con i dati del viaggio
     */
    private fun updateMapWithTripData(trip: com.example.travelcompanion.database.models.Trip?) {
        if (trip == null) return
        googleMap?.let { map ->
            map.clear()
            // Carica le fasi del viaggio per ottenere le coordinate
            viewModel.getTripPhases(trip.id).observe(viewLifecycleOwner) { phases ->
                if (phases.isNotEmpty()) {
                    val startPhase = phases.first()
                    viewModel.getLocationById(startPhase.locationId).observe(viewLifecycleOwner) { startLocation ->
                        startLocation?.let { location ->
                            val startLatLng = LatLng(location.latitude, location.longitude)
                            map.addMarker(
                                MarkerOptions()
                                    .position(startLatLng)
                                    .title("Inizio viaggio")
                                    .snippet("Partenza: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(trip.startDate))}")
                            )
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
                        }
                    }
                } else {
                    // Nessuna fase trovata, usa la posizione corrente dell'utente
                    LocationUtils.getCurrentLocation(
                        requireContext(),
                        onSuccess = { currentLatLng ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(currentLatLng)
                                    .title("Posizione corrente")
                                    .snippet("Punto di partenza del viaggio")
                            )
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        },
                        onFailure = { defaultLatLng ->
                            map.addMarker(
                                MarkerOptions()
                                    .position(defaultLatLng)
                                    .title("Posizione di default")
                                    .snippet("Impossibile ottenere la posizione corrente")
                            )
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLatLng, 10f))
                        }
                    )
                }
            }
            // Reset user path polyline
            userPath?.remove()
            userPathPoints.clear()
        }
    }

    /**
     * Setup LocationCallback for real-time updates
     */
    private fun setupLocationCallback() {
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val map = googleMap ?: return
                for (location in locationResult.locations) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    userPathPoints.add(latLng)
                    // Draw or update polyline
                    if (userPath == null) {
                        userPath = map.addPolyline(
                            PolylineOptions()
                                .addAll(userPathPoints)
                                .color(Color.MAGENTA)
                                .width(7f)
                        )
                    } else {
                        userPath?.points = userPathPoints
                    }
                    // Move camera to current location
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 16f))

                    // Salva la fase e aggiorna la distanza
                    viewModel.savePhaseAndUpdateDistance(tripId, location.latitude, location.longitude)
                }
            }
        }
    }

    /**
     * Start location updates for real-time tracking
     */
    private fun startLocationUpdates() {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY, 30000L
        ).apply {
            setMinUpdateIntervalMillis(1500L)
        }.build()

        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permesso non concesso.
            return
        }
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
    }

    /**
     * Stop location updates when not needed
     */
    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    /**
     * Osserva i dati del viaggio e aggiorna la UI
     */
    private suspend fun observeTrip() {
        // Carica i dati del viaggio usando il viewModel
        viewModel.getTripById(tripId).observe(viewLifecycleOwner) { trip ->
            trip?.let {
                updateUI(it)
            }
        }
    }

    /**
     * Aggiorna la UI con i dati del viaggio
     */
    private fun updateUI(trip: com.example.travelcompanion.database.models.Trip?) {
        if (trip == null) return
        
        view?.let { view ->
            val tvTripTitle = view.findViewById<TextView>(R.id.tvHomeTripTitle)
            val tvStartDate = view.findViewById<TextView>(R.id.tv_start_date)
            val tvEndDate = view.findViewById<TextView>(R.id.tv_end_date)
            val tvDistance = view.findViewById<TextView>(R.id.tv_distance)

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

            tvTripTitle.text = trip.title
            tvStartDate.text = dateFormat.format(Date(trip.startDate))
            tvEndDate.text = if (trip.endDate != 0L) dateFormat.format(Date(trip.endDate)) else "In corso"
            tvDistance.text = "Distanza: ${String.format("%.1f", trip.distance)} m"

            // Gestione cronometro - calcola il tempo trascorso dall'inizio del viaggio
            if (trip.isActive && !isChronoRunning) {
                val currentTime = System.currentTimeMillis()
                val elapsedTime = currentTime - trip.startDate
                chronometer.base = android.os.SystemClock.elapsedRealtime() - elapsedTime
                chronometer.start()
                isChronoRunning = true
                Log.d("HomeActiveTripFragment", "Chronometer started - elapsed time: ${elapsedTime}ms")
            }
            if (!trip.isActive && isChronoRunning) {
                chronometer.stop()
                isChronoRunning = false
                Log.d("HomeActiveTripFragment", "Chronometer stopped")
            }
        }
    }

    /**
     * Ferma il viaggio attivo
     */
    private fun stopTrip() {
        viewModel.stopTrip(tripId) { success ->
            requireActivity().runOnUiThread {
                if (success) {
                    Toast.makeText(requireContext(), "Viaggio terminato", Toast.LENGTH_SHORT).show()
                    // Naviga alla schermata "I miei viaggi"
                    findNavController().navigate(R.id.nav_my_trips)
                } else {
                    Toast.makeText(requireContext(), "Errore durante la chiusura del viaggio", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    /**
     * Verifica il permesso della fotocamera e scatta una foto
     */
    private fun checkCameraPermissionAndTakePicture() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                takePicture()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }

    /**
     * Scatta una foto
     */
    private fun takePicture() {
        imageUri = viewModel.createImageUri(requireContext())
        imageUri?.let { uri ->
            takePictureLauncher.launch(uri)
        }
    }

    /**
     * Salva l'immagine al viaggio corrente
     */
    private fun saveImageToCurrentPhase(uri: Uri) {
        viewModel.saveImageToTrip(tripId, uri.toString())
        Toast.makeText(requireContext(), "Foto salvata!", Toast.LENGTH_SHORT).show()
    }

    /**
     * Mostra il dialog per aggiungere una nota
     */
    private fun showAddNoteDialog() {
        val dialog = AddNoteDialogFragment()
        dialog.setOnNoteAddedListener { note ->
            viewModel.saveNoteToTrip(tripId, note)
            Toast.makeText(requireContext(), "Nota salvata!", Toast.LENGTH_SHORT).show()
        }
        dialog.show(childFragmentManager, "AddNoteDialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChronoRunning) {
            chronometer.stop()
        }
        stopLocationUpdates()
    }
}
