package com.example.travelcompanion.ui.settings

import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import androidx.activity.result.contract.ActivityResultContracts

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.BuildConfig
import com.example.travelcompanion.database.TravelDatabase
import com.example.travelcompanion.database.entities.Location
import com.example.travelcompanion.database.entities.User
import com.example.travelcompanion.databinding.FragmentSettingsBinding
import com.example.travelcompanion.repository.TravelRepository
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

class SettingsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TravelRepository
    private var currentUser: User? = null
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            enableUserLocation()
        } else {
            Toast.makeText(requireContext(), "Permesso posizione negato", Toast.LENGTH_SHORT).show()
        }
    }

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

        val database = TravelDatabase.getDatabase(requireContext())
        repository = TravelRepository(
            database.locationDao(),
            database.userDao(),
            database.tripDao()
        )

        setupMap()
        setupViews()
        loadUserData()

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
    }

    private fun setupMap() {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map
        googleMap?.uiSettings?.isZoomControlsEnabled = true
        googleMap?.uiSettings?.isMyLocationButtonEnabled = true

        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            enableUserLocation()
        } else {
            locationPermissionRequest.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }

        val defaultLocation = LatLng(44.4949, 11.3426)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        googleMap?.setOnMapClickListener { latLng ->
            selectedLocation = latLng

            googleMap?.clear()
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Casa")
            )

            binding.tvSelectedCoordinates.text =
                "Coordinate selezionate: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}"
        }
    }

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

    private fun setupViews() {
        binding.btnSaveSettings.setOnClickListener {
            saveUserData()
        }

        binding.btnResetApp.setOnClickListener {
            resetApp()
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

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                val currentUser = repository.getUserById(1) 

                currentUser?.let { user ->
                    binding.etUsername.setText(user.name)
                    binding.etEmail.setText(user.email)

                    user.homeLocationId?.let { locationId ->
                        val location = repository.getLocationById(locationId)
                        location?.let {
                            val homeLatLng = LatLng(it.latitude, it.longitude)
                            selectedLocation = homeLatLng

                            googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(homeLatLng, 15f))
                            googleMap?.addMarker(
                                MarkerOptions()
                                    .position(homeLatLng)
                                    .title("Casa")
                            )

                            binding.tvSelectedCoordinates.text =
                                "Coordinate selezionate: ${String.format("%.6f", it.latitude)}, ${String.format("%.6f", it.longitude)}"
                        }
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error loading user data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserData() {
        val username = binding.etUsername.text.toString().trim()
        val email = binding.etEmail.text.toString().trim()

        if (username.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter a username", Toast.LENGTH_SHORT).show()
            return
        }

        if (email.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter an email", Toast.LENGTH_SHORT).show()
            return
        }

        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(requireContext(), "Please enter a valid email", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                val homeLocationId = selectedLocation?.let { location ->
                    saveHomeLocation(location)
                }

                if (currentUser != null) {
                    val updatedUser = currentUser!!.copy(
                        name = username,
                        email = email,
                        homeLocationId = homeLocationId
                    )
                    repository.updateUser(updatedUser)
                    currentUser = updatedUser
                } else {
                    val newUser = User(
                        id = 1,
                        name = username,
                        email = email,
                        homeLocationId = homeLocationId
                    )
                    repository.insertUser(newUser)
                    currentUser = newUser
                }

                Toast.makeText(requireContext(), "Settings saved successfully", Toast.LENGTH_SHORT).show()

            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error saving settings: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun saveHomeLocation(latLng: LatLng): Long? {
        return try {
            val location = Location(
                latitude = latLng.latitude,
                longitude = latLng.longitude
            )
            repository.insertLocationAndGetId(location)
        } catch (e: Exception) {
            null
        }
    }

    private fun resetApp() {
        lifecycleScope.launch {
            try {
                repository.deleteAllUsers()
                repository.deleteAllLocations()
                repository.deleteAllTrips()

                binding.etUsername.setText("")
                binding.etEmail.setText("")
                binding.tvSelectedCoordinates.text = "Nessuna posizione selezionata"
                selectedLocation = null
                currentUser = null

                googleMap?.clear()

                Toast.makeText(requireContext(), "App reset successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error resetting app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAddress(address: String) {
        lifecycleScope.launch {
            val latLng = geocodeAddress(address)
            if (latLng != null) {
                selectedLocation = latLng
                googleMap?.clear()
                googleMap?.addMarker(
                    MarkerOptions()
                        .position(latLng)
                        .title("Casa")
                )
                googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                binding.tvSelectedCoordinates.text =
                    "Coordinate selezionate: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}"
            } else {
                Toast.makeText(requireContext(), "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun geocodeAddress(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val urlString =
                "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=$geocodingApiKey"
            val response = URL(urlString).readText()
            val jsonObject = JSONObject(response)
            val results = jsonObject.getJSONArray("results")
            if (results.length() > 0) {
                val location =
                    results.getJSONObject(0).getJSONObject("geometry").getJSONObject("location")
                val lat = location.getDouble("lat")
                val lng = location.getDouble("lng")
                return@withContext LatLng(lat, lng)
            }
        } catch (e: Exception) {
        }
        return@withContext null
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}