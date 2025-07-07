package com.example.travelcompanion.ui.settings

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.BuildConfig
import com.example.travelcompanion.database.TravelDatabase
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.databinding.FragmentSettingsBinding
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.ui.common.ProfilePicturePickerFragment
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

    private lateinit var viewModel: SettingsViewModel
    private var googleMap: GoogleMap? = null
    private var selectedLocation: LatLng? = null
    private var currentProfilePictureUri: Uri? = null

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
        val repository = TravelRepository(
            database.userDao(),
            database.locationDao(),
            database.tripDao(),
            database.imageDao() // Add this
        )
        val factory = SettingsViewModelFactory(repository)
        viewModel = ViewModelProvider(this, factory)[SettingsViewModel::class.java]

        setupProfilePicturePicker()
        setupMap()
        setupViews()
        observeViewModel()

        viewModel.loadUserData()
    }

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

    private fun setupMap() {
        val mapFragment = SupportMapFragment.newInstance()
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, mapFragment)
            .commit()
        mapFragment.getMapAsync(this)
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        val defaultLocation = LatLng(44.4949, 11.3426)
        googleMap?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))

        googleMap?.setOnMapClickListener { latLng ->
            viewModel.setHomeLocation(latLng)
        }
    }

    private fun setupViews() {
        binding.btnSaveSettings.setOnClickListener {
            val username = binding.etUsername.text.toString().trim()
            val email = binding.etEmail.text.toString().trim()
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
        }

        viewModel.profilePictureUri.observe(viewLifecycleOwner) { uri ->
            currentProfilePictureUri = uri
            // Update the profile picture picker fragment
            val profilePictureFragment = childFragmentManager.findFragmentById(binding.profilePictureContainer.id) as? ProfilePicturePickerFragment
            profilePictureFragment?.setCurrentImage(uri)
        }

        viewModel.message.observe(viewLifecycleOwner) { msg ->
            Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
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