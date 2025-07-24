package com.example.travelcompanion.ui.common

import android.location.Geocoder
import com.example.travelcompanion.databinding.FragmentAddressSearchBinding
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import com.example.travelcompanion.R
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
import android.text.InputFilter

/**
 * Fragment per la ricerca di indirizzi e la visualizzazione su mappa
 * Permette di cercare indirizzi tramite Google API e ottenere la posizione corrente
 */
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

        binding.etAddressSearch.filters = arrayOf(InputFilter.LengthFilter(200))

        childFragmentManager.beginTransaction()
            .replace(R.id.mapContainer, MapFragment())
            .commit()

        binding.btnSearchAddress.setOnClickListener {
            val raw = binding.etAddressSearch.text.toString()
            when {
                raw.length > 200 -> {
                    Toast.makeText(requireContext(), getString(R.string.error_too_long), Toast.LENGTH_SHORT).show()
                }
                raw.trim().isEmpty() -> {
                    Toast.makeText(requireContext(), getString(R.string.insert_address), Toast.LENGTH_SHORT).show()
                }
                else -> {
                    searchAddress(raw.trim())
                }
            }
        }
    }

    /**
     * Cerca un indirizzo utilizzando Google Geocoding API
     */
    private fun searchAddress(address: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val latLng = geocodeAddress(address)
            if (latLng != null) {
                binding.tvSelectedAddress.text = getString(R.string.selected_address, address)
                onAddressSelectedListener?.invoke(address, latLng)
                val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
                mapFragment?.showLocation(latLng, address)
            } else {
                Toast.makeText(requireContext(), getString(R.string.address_not_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Converte un indirizzo in coordinate geografiche tramite Google Geocoding API
     */
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

    /**
     * Abilita o disabilita l'input dell'utente
     */
    fun setInputEnabled(enabled: Boolean) {
        binding.etAddressSearch.isEnabled = enabled
        binding.btnSearchAddress.isEnabled = enabled
        binding.mapContainer.isClickable = enabled
        binding.mapContainer.isFocusable = enabled
    }

    /**
     * Imposta il listener per quando viene selezionato un indirizzo
     */
    fun setOnAddressSelectedListener(listener: (String, LatLng) -> Unit) {
        onAddressSelectedListener = listener
    }

    /**
     * Imposta il testo dell'indirizzo nell'input
     */
    fun setAddressText(address: String) {
        binding.etAddressSearch.setText(address)
        binding.tvSelectedAddress.text = getString(R.string.selected_address, address)
    }

    /**
     * Ottiene la posizione corrente dell'utente e la converte in un indirizzo leggibile
     */
    fun getUserLocation(onLocationReady: (String, LatLng) -> Unit) {
        val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(requireActivity())

        val context = requireContext()
        val permission = android.Manifest.permission.ACCESS_FINE_LOCATION
        val permissionGranted = androidx.core.content.ContextCompat.checkSelfPermission(context, permission) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (permissionGranted) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    val latLng = LatLng(location.latitude, location.longitude)
                    val geocoder = Geocoder(context, java.util.Locale.getDefault())
                    
                    try {
                        @Suppress("DEPRECATION")
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val addressText = if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            
                            val addressParts = mutableListOf<String>()
                            
                            val street = listOfNotNull(
                                address.thoroughfare?.takeIf { it.isNotBlank() },
                                address.subThoroughfare?.takeIf { it.isNotBlank() }
                            ).joinToString(" ")

                            if (street.isNotBlank()) addressParts.add(street)
                            
                            address.locality?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            address.adminArea?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            address.postalCode?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            address.countryName?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            
                            if (addressParts.isNotEmpty()) {
                                addressParts.joinToString(", ")
                            } else {
                                getString(R.string.position, location.latitude, location.longitude)
                            }
                        } else {
                            getString(R.string.position, location.latitude, location.longitude)
                        }

                        binding.etAddressSearch.setText(addressText)
                        binding.tvSelectedAddress.text = getString(R.string.selected_address, addressText)
                        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
                        mapFragment?.showLocation(latLng, addressText)
                        onAddressSelectedListener?.invoke(addressText, latLng)
                        onLocationReady(addressText, latLng)
                    } catch (e: Exception) {
                        val fallbackAddress = getString(R.string.position, location.latitude, location.longitude)
                        binding.etAddressSearch.setText(fallbackAddress)
                        binding.tvSelectedAddress.text = getString(R.string.selected_address, fallbackAddress)
                        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
                        mapFragment?.showLocation(latLng, fallbackAddress)
                        onAddressSelectedListener?.invoke(fallbackAddress, latLng)
                        onLocationReady(fallbackAddress, latLng)
                    }
                } else {
                    Toast.makeText(context, getString(R.string.position_not_available), Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, getString(R.string.location_error, exception.message), Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, getString(R.string.location_permission_denied), Toast.LENGTH_SHORT).show()
        }
    }
}