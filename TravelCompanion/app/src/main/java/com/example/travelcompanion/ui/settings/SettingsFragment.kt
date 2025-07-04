package com.example.travelcompanion.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
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
import kotlinx.coroutines.launch

class SettingsFragment : Fragment(), OnMapReadyCallback {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private lateinit var repository: TravelRepository
    private var currentUser: User? = null
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null

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

        // Initialize repository
        val database = TravelDatabase.getDatabase(requireContext())
        repository = TravelRepository(
            database.locationDao(),
            database.userDao(),
            database.tripDao()
        )

        setupMap()
        setupViews()
        loadUserData()
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

        // Set default location (you can change this to your preferred default)
        val defaultLocation = LatLng(45.4642, 9.1900) // Milano, Italy
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        // Set up map click listener
        googleMap?.setOnMapClickListener { latLng ->
            selectedLocation = latLng

            // Clear previous markers and add new one
            googleMap?.clear()
            googleMap?.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Casa")
            )

            // Update coordinates text
            binding.tvSelectedCoordinates.text =
                "Coordinate selezionate: ${String.format("%.6f", latLng.latitude)}, ${String.format("%.6f", latLng.longitude)}"
        }
    }

    private fun setupViews() {
        // Save button functionality
        binding.btnSaveSettings.setOnClickListener {
            saveUserData()
        }

        // Reset app button functionality
        binding.btnResetApp.setOnClickListener {
            resetApp()
        }
    }

    private fun loadUserData() {
        lifecycleScope.launch {
            try {
                // Assuming we always work with the first user (you might need to implement proper user management)
                val users = repository.getAllUsers()
                currentUser = users.firstOrNull()

                currentUser?.let { user ->
                    binding.etUsername.setText(user.name)
                    binding.etEmail.setText(user.email)

                    // Load home location if available
                    user.homeLocationId?.let { locationId ->
                        val location = repository.getLocationById(locationId)
                        location?.let {
                            val homeLatLng = LatLng(it.latitude, it.longitude)
                            selectedLocation = homeLatLng

                            // Move camera to home location and add marker
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
                    // Update existing user
                    val updatedUser = currentUser!!.copy(
                        name = username,
                        email = email,
                        homeLocationId = homeLocationId
                    )
                    repository.updateUser(updatedUser)
                    currentUser = updatedUser
                } else {
                    // Create new user
                    val newUser = User(
                        id = 0, // Auto-generate
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

                // Clear UI
                binding.etUsername.setText("")
                binding.etEmail.setText("")
                binding.tvSelectedCoordinates.text = "Nessuna posizione selezionata"
                selectedLocation = null
                currentUser = null

                // Clear map
                googleMap?.clear()

                Toast.makeText(requireContext(), "App reset successfully", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Error resetting app", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}