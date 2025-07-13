package com.example.travelcompanion.ui.settings

import android.net.Uri
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.activity.result.contract.ActivityResultContracts
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.BuildConfig
import com.example.travelcompanion.databinding.FragmentSettingsBinding
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.ui.common.ProfilePicturePickerFragment
import com.example.travelcompanion.utils.LocationUtils
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URLEncoder
import java.net.URL
import android.util.Patterns
import androidx.core.widget.addTextChangedListener
import com.example.travelcompanion.R

/**
 * Fragment per le impostazioni dell'utente
 * Gestisce profilo utente, posizione casa e configurazioni app
 */
class SettingsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var viewModel: SettingsViewModel
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private var currentProfilePictureUri: Uri? = null
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    /**
     * Launcher per la richiesta del permesso di localizzazione
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

    /**
     * Chiave API per il servizio di geocoding di Google
     */
    private val geocodingApiKey: String
        get() = BuildConfig.MAPS_API_KEY

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())

        setupViewModel()
        setupProfilePicturePicker()
        setupMap()
        setupViews()
        
        observeViewModel()
        
        binding.etUsername.addTextChangedListener { updateSaveButtonState() }
        binding.etEmail.addTextChangedListener { updateSaveButtonState() }
        
        viewModel.loadUserData()
    }

    /**
     * Configura il ViewModel
     */
    private fun setupViewModel() {
        val repository = TravelRepository.create(requireContext())
        val factory = SettingsViewModelFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]
    }

    /**
     * Configura il fragment per la selezione della foto profilo
     */
    private fun setupProfilePicturePicker() {
        val profilePicturePickerFragment = ProfilePicturePickerFragment()

        childFragmentManager.beginTransaction()
            .replace(binding.profilePictureContainer.id, profilePicturePickerFragment)
            .commit()

        profilePicturePickerFragment.setOnImageSelectedListener { uri ->
            currentProfilePictureUri = uri
            viewModel.updateProfilePicture(uri)
        }
    }

    /**
     * Configura la mappa per la selezione della posizione casa
     */
    private fun setupMap() {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    /**
     * Callback chiamato quando la mappa è pronta
     */
    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (LocationUtils.hasLocationPermission(requireContext())) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        // Imposta Bologna come posizione di default
        val defaultLocation = LocationUtils.getDefaultBolognaLocation()
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Prova a ottenere la posizione corrente
        LocationUtils.getCurrentLocationForSettings(
            requireContext(),
            onSuccess = { currentLatLng ->
                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            },
            onFailure = { _ ->
                // Mantiene la posizione di default Bologna se il GPS non è disponibile
            }
        )

        googleMap?.setOnMapClickListener { latLng ->
            viewModel.setHomeLocation(latLng)
        }
    }

    /**
     * Abilita la visualizzazione della posizione utente sulla mappa
     */
    private fun enableUserLocation() {
        try {
            googleMap?.isMyLocationEnabled = true
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val currentLatLng = LatLng(location.latitude, location.longitude)
                    googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 16f))
                }
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * Configura i listener per i pulsanti e la validazione
     */
    private fun setupViews() {
        binding.btnSaveSettings.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(requireContext(), getString(R.string.error_invalid_email), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val homeLocation = viewModel.homeLocation.value
            viewModel.saveUser(username, email, homeLocation, currentProfilePictureUri)
        }

        binding.btnResetApp.setOnClickListener {
            viewModel.resetApp()
        }

        binding.btnSearchAddress.setOnClickListener {
            val address = binding.etAddressSearch.text.toString().trim()
            if (address.isNotEmpty()) {
                searchAddress(address)
            } else {
                Toast.makeText(requireContext(), "Inserisci un indirizzo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Configura gli observer per i dati del ViewModel
     */
    private fun observeViewModel() {
        viewModel.user.observe(viewLifecycleOwner) { user ->
            binding.etUsername.setText(user?.name ?: "")
            binding.etEmail.setText(user?.email ?: "")
        }

        viewModel.homeLocation.observe(viewLifecycleOwner) { latLng ->
            if (latLng != null) {
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                googleMap?.clear()
                googleMap?.addMarker(MarkerOptions().position(latLng).title("Casa"))
                binding.tvSelectedCoordinates.text =
                    "Coordinate selezionate: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}"
            } else {
                binding.tvSelectedCoordinates.text = "Nessuna posizione selezionata"
                googleMap?.clear()
            }
            updateSaveButtonState()
        }

        viewModel.profilePictureUri.observe(viewLifecycleOwner) { uri ->
            currentProfilePictureUri = uri
            val profilePictureFragment = childFragmentManager.findFragmentById(binding.profilePictureContainer.id) as? ProfilePicturePickerFragment
            profilePictureFragment?.setCurrentImage(uri)
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Cerca un indirizzo e aggiorna la posizione casa
     */
    private fun searchAddress(address: String) {
        lifecycleScope.launch {
            val latLng = geocodeAddress(address)
            if (latLng != null) {
                // Aggiorna il ViewModel con la nuova posizione
                viewModel.setHomeLocation(latLng)

                googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))

                Toast.makeText(requireContext(), "Indirizzo trovato e selezionato", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(requireContext(), "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Geocodifica un indirizzo in coordinate geografiche usando Google Geocoding API
     */
    private suspend fun geocodeAddress(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val urlString =
                "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$geocodingApiKey"
            val response = URL(urlString).readText()
            val jsonObject = JSONObject(response)
            val results = jsonObject.getJSONArray("results")
            if (results.length() > 0) {
                val location = results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                val lat = location.getDouble("lat")
                val lng = location.getDouble("lng")
                return@withContext LatLng(lat, lng)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }

    /**
     * Abilita/disabilita il bottone Salva in base alle condizioni di validazione
     */
    private fun updateSaveButtonState() {
        val usernameValid = binding.etUsername.text.toString().trim().isNotEmpty()
        val emailText = binding.etEmail.text.toString().trim()
        val emailValid = Patterns.EMAIL_ADDRESS.matcher(emailText).matches()
        val addressValid = viewModel.homeLocation.value != null
        binding.btnSaveSettings.isEnabled = usernameValid && emailValid && addressValid
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}