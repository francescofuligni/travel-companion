package com.example.travelcompanion.ui.common

import com.example.travelcompanion.databinding.FragmentAddressSearchBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.BuildConfig
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder
import androidx.fragment.app.Fragment
import android.view.View
import android.view.ViewGroup

class AddressSearchFragment : Fragment() {

    private var _binding: FragmentAddressSearchBinding? = null
    private val binding get() = _binding!!

    private var onAddressSelectedListener: ((String, LatLng) -> Unit)? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddressSearchBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Carica MapFragment nel container
        childFragmentManager.beginTransaction()
            .replace(com.example.travelcompanion.R.id.mapContainer, MapFragment())
            .commit()

        binding.btnSearchAddress.setOnClickListener {
            val address = binding.etAddressSearch.text.toString().trim()
            if (address.isNotEmpty()) {
                searchAddress(address)
            } else {
                Toast.makeText(requireContext(), "Inserisci un indirizzo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAddress(address: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val latLng = geocodeAddress(address)
            if (latLng != null) {
                binding.tvSelectedAddress.text = "Indirizzo selezionato: $address"
                onAddressSelectedListener?.invoke(address, latLng)
                val mapFragment = childFragmentManager.findFragmentById(com.example.travelcompanion.R.id.mapContainer) as? MapFragment
                mapFragment?.showLocation(latLng, address)
            } else {
                Toast.makeText(requireContext(), "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private suspend fun geocodeAddress(address: String): LatLng? = withContext(Dispatchers.IO) {
        try {
            val encodedAddress = URLEncoder.encode(address, "UTF-8")
            val urlString = "https://maps.googleapis.com/maps/api/geocode/json?address=$encodedAddress&key=${BuildConfig.MAPS_API_KEY}"
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

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setInputEnabled(enabled: Boolean) {
        binding.etAddressSearch.isEnabled = enabled
        binding.btnSearchAddress.isEnabled = enabled
        binding.mapContainer.isClickable = enabled
        binding.mapContainer.isFocusable = enabled
    }

    fun setOnAddressSelectedListener(listener: (String, LatLng) -> Unit) {
        onAddressSelectedListener = listener
    }

    fun setAddressText(address: String) {
        binding.etAddressSearch.setText(address)
        binding.tvSelectedAddress.text = "Indirizzo selezionato: $address"
    }

    fun getUserLocation(onLocationReady: (String, LatLng) -> Unit) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())

        val context = requireContext()
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val geocoder = android.location.Geocoder(context, java.util.Locale.getDefault())
                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                    val addressText = if (addresses != null && addresses.isNotEmpty()) {
                        val address = addresses[0]
                        listOfNotNull(
                            address.thoroughfare,
                            address.subThoroughfare,
                            address.locality,
                            address.adminArea,
                            address.postalCode,
                            address.countryName
                        ).joinToString(", ")
                    } else {
                        "Indirizzo non disponibile"
                    }

                    binding.etAddressSearch.setText(addressText)
                    binding.tvSelectedAddress.text = "Indirizzo selezionato: $addressText"
                    val mapFragment = childFragmentManager.findFragmentById(com.example.travelcompanion.R.id.mapContainer) as? MapFragment
                    mapFragment?.showLocation(latLng, addressText)
                    onAddressSelectedListener?.invoke(addressText, latLng)
                    onLocationReady(addressText, latLng)
                } else {
                    Toast.makeText(context, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            Toast.makeText(context, "Permesso posizione non concesso", Toast.LENGTH_SHORT).show()
        }
    }
}