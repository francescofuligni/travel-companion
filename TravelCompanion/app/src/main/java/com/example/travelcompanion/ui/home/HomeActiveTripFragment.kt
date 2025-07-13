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
import com.example.travelcompanion.ui.common.AddNoteDialog
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.location.LocationServices
import com.example.travelcompanion.utils.LocationUtils

import androidx.core.app.ActivityCompat
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.Trip
import com.example.travelcompanion.database.models.TripPhase

import android.os.SystemClock
import com.example.travelcompanion.databinding.FragmentHomeActiveTripBinding
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

private fun Fragment.toast(msg: String) =
    Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()

private fun Chronometer.startFrom(startDateMillis: Long) {
    val elapsed = System.currentTimeMillis() - startDateMillis
    base = SystemClock.elapsedRealtime() - elapsed
    start()
}

private fun GoogleMap.setupDefaults() {
    uiSettings.isZoomControlsEnabled = true
    uiSettings.isMyLocationButtonEnabled = true
}

/**
 * Fragment che mostra i dettagli di un viaggio attivo in corso
 * Include una mappa per visualizzare il percorso in tempo reale
 */
class HomeActiveTripFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentHomeActiveTripBinding? = null
    private val binding get() = _binding!!

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
            toast("Permesso fotocamera negato")
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

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentHomeActiveTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (tripId == -1L) {
            Log.e("HomeActiveTripFragment", "Trip ID non valido")
            return
        }

        setupViews()
        setupMap()
        observeTrip()
    }

    /**
     * Configura le view e i listener
     */
    private fun setupViews() {
        chronometer = binding.chronometer
        binding.btnStop.setOnClickListener { stopTrip() }
        binding.btnPhoto.setOnClickListener { checkCameraPermissionAndTakePicture() }
        binding.btnNote.setOnClickListener { showAddNoteDialog() }
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
        googleMap = map.apply { setupDefaults() }
    }


    /**
     * Aggiorna la mappa con i dati del viaggio
     */
    private fun updateMapWithTripData(trip: Trip?) {
        if (trip == null) return
        googleMap?.let { map ->
            map.clear()
            // Carica le fasi del viaggio per ottenere le coordinate
            viewModel.getTripPhases(trip.id).observe(viewLifecycleOwner) { phases ->
                if (phases.isNotEmpty()) {
                    val startPhase:TripPhase = phases.first()
                    viewModel.getLocationById(startPhase.locationId ).observe(viewLifecycleOwner) { startLocation->
                        startLocation?.let { location  ->
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
        }
    }

    /**
     * Osserva i dati del viaggio e aggiorna la UI
     */
    private fun observeTrip() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.getTripById(tripId).observe(viewLifecycleOwner) { trip ->
                trip?.let {
                    updateUI(it)
                    updateMapWithTripData(it)
                }
            }
        }
    }

    /**
     * Aggiorna la UI con i dati del viaggio
     */
    private fun updateUI(trip: Trip?) {
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
                chronometer.startFrom(trip.startDate)
                isChronoRunning = true
                Log.d("HomeActiveTripFragment", "Chronometer started - elapsed time: ${System.currentTimeMillis() - trip.startDate}ms")
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
                    toast("Viaggio terminato")
                    // Naviga alla schermata "I miei viaggi"
                    findNavController().navigate(R.id.nav_my_trips)
                } else {
                    toast("Errore durante la chiusura del viaggio")
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
        toast("Foto salvata!")
    }

    /**
     * Mostra il dialog per aggiungere una nota
     */
    private fun showAddNoteDialog() {
        val dialog = AddNoteDialog()
        dialog.setOnNoteAddedListener { note ->
            viewModel.saveNoteToTrip(tripId, note)
            toast("Nota salvata!")
        }
        dialog.show(childFragmentManager, "AddNoteDialog")
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChronoRunning) {
            chronometer.stop()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
