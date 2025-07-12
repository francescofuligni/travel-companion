package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Chronometer
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
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.CameraUpdateFactory

/**
 * Fragment che mostra i dettagli di un viaggio attivo in corso
 * Include una mappa per visualizzare il percorso in tempo reale
 */
class HomeActiveTripFragment : Fragment(), OnMapReadyCallback {

    private var tripId: Long = -1L
    private lateinit var viewModel: HomeActiveTripViewModel
    private lateinit var chronometer: Chronometer
    private var isChronoRunning = false
    private var googleMap: GoogleMap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getLong("tripId") ?: -1L
        Log.d("HomeActiveTripFragment", "Trip ID: $tripId")
        
        val repository = TravelRepository.create(requireContext())
        val factory = HomeActiveTripViewModelFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[HomeActiveTripViewModel::class.java]
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
        observeTrip()
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
        val tvDuration = view.findViewById<TextView>(R.id.tv_duration_label)
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
            // TODO: Implementare cattura foto
            Toast.makeText(requireContext(), "Funzione foto in sviluppo", Toast.LENGTH_SHORT).show()
        }
        
        // Note button
        noteButton.setOnClickListener {
            Log.d("HomeActiveTripFragment", "Note button clicked")
            // TODO: Implementare aggiunta nota
            Toast.makeText(requireContext(), "Funzione note in sviluppo", Toast.LENGTH_SHORT).show()
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
    private fun updateMapWithTripData(trip: com.example.travelcompanion.database.models.Trip) {
        googleMap?.let { map ->
            // Pulisci la mappa precedente
            map.clear()
            
            // Carica le fasi del viaggio per ottenere le coordinate
            viewModel.getTripPhases(trip.id).observe(viewLifecycleOwner) { phases ->
                if (phases.isNotEmpty()) {
                    // Prendi la prima fase come punto di partenza
                    val startPhase = phases.first()
                    viewModel.getLocationById(startPhase.locationId).observe(viewLifecycleOwner) { startLocation ->
                        startLocation?.let { location ->
                            val startLatLng = LatLng(location.latitude, location.longitude)
                            
                            // Aggiungi marker per il punto di partenza
                            map.addMarker(
                                MarkerOptions()
                                    .position(startLatLng)
                                    .title("Inizio viaggio")
                                    .snippet("Partenza: ${SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date(trip.startDate))}")
                            )
                            
                            // Centra la mappa sul punto di partenza
                            map.animateCamera(CameraUpdateFactory.newLatLngZoom(startLatLng, 15f))
                            
                            // TODO: Aggiungi percorso completo se ci sono più fasi
                            // TODO: Traccia la posizione corrente dell'utente
                        }
                    }
                } else {
                    // Nessuna fase trovata, usa una posizione di default
                    val defaultLocation = LatLng(44.0043, 12.6560) // Riccione come fallback
                    map.addMarker(
                        MarkerOptions()
                            .position(defaultLocation)
                            .title("Posizione di default")
                    )
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
                }
            }
        }
    }

    /**
     * Osserva i dati del viaggio e aggiorna la UI
     */
    private fun observeTrip() {
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
    private fun updateUI(trip: com.example.travelcompanion.database.models.Trip) {
        view?.let { view ->
            val tvTripTitle = view.findViewById<TextView>(R.id.tvHomeTripTitle)
            val tvStartDate = view.findViewById<TextView>(R.id.tv_start_date)
            val tvEndDate = view.findViewById<TextView>(R.id.tv_end_date)
            val tvDistance = view.findViewById<TextView>(R.id.tv_distance)
            val tvDuration = view.findViewById<TextView>(R.id.tv_duration_label)
            
            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            
            tvTripTitle.text = trip.title ?: "Viaggio in corso..."
            tvStartDate.text = dateFormat.format(Date(trip.startDate))
            tvEndDate.text = if (trip.endDate != 0L) dateFormat.format(Date(trip.endDate)) else "In corso"
            tvDistance.text = "Distanza: ${String.format("%.1f", trip.distance)} km"
            
            // Calcola la durata
            val duration = if (trip.endDate != 0L) {
                trip.endDate - trip.startDate
            } else {
                System.currentTimeMillis() - trip.startDate
            }
            
            val hours = duration / (1000 * 60 * 60)
            val minutes = (duration % (1000 * 60 * 60)) / (1000 * 60)
            val seconds = (duration % (1000 * 60)) / 1000
            
            tvDuration.text = "Durata: ${String.format("%02d:%02d:%02d", hours, minutes, seconds)}"
            
            // Gestione cronometro
            if (trip.endDate == 0L && !isChronoRunning) {
                chronometer.base = android.os.SystemClock.elapsedRealtime() - duration
                chronometer.start()
                isChronoRunning = true
            } else if (trip.endDate != 0L && isChronoRunning) {
                chronometer.stop()
                isChronoRunning = false
            }
        }
    }

    /**
     * Ferma il viaggio attivo
     */
    private fun stopTrip() {
        viewModel.stopTrip(tripId)
        Toast.makeText(requireContext(), "Viaggio terminato", Toast.LENGTH_SHORT).show()
        
        // Naviga indietro o alla schermata home
        findNavController().popBackStack()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (isChronoRunning) {
            chronometer.stop()
        }
    }
}
