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

        // Limite massimo di 200 caratteri per l'indirizzo
        binding.etAddressSearch.filters = arrayOf(InputFilter.LengthFilter(200))

        // Carica MapFragment nel container
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
                    Toast.makeText(requireContext(), "Inserisci un indirizzo", Toast.LENGTH_SHORT).show()
                }
                else -> {
                    searchAddress(raw.trim())
                }
            }
        }
    }

    private fun searchAddress(address: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            val latLng = geocodeAddress(address)
            if (latLng != null) {
                binding.tvSelectedAddress.text = "Indirizzo selezionato: $address"
                onAddressSelectedListener?.invoke(address, latLng)
                val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
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

    /**
     * Ottiene la posizione corrente dell'utente e la converte in un indirizzo leggibile
     * @param onLocationReady callback chiamato quando la posizione è pronta
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
                        val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val addressText = if (!addresses.isNullOrEmpty()) {
                            val address = addresses[0]
                            
                            // Costruisci l'indirizzo pezzo per pezzo, controllando che ogni campo non sia null o vuoto
                            val addressParts = mutableListOf<String>()
                            
                            // Numero civico e via
                            val street = listOfNotNull(
                                address.thoroughfare?.takeIf { it.isNotBlank() },
                                address.subThoroughfare?.takeIf { it.isNotBlank() }
                            ).joinToString(" ")
                            if (street.isNotBlank()) addressParts.add(street)
                            
                            // Località
                            address.locality?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            
                            // Provincia/Stato
                            address.adminArea?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            
                            // CAP
                            address.postalCode?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            
                            // Paese
                            address.countryName?.takeIf { it.isNotBlank() }?.let { addressParts.add(it) }
                            
                            if (addressParts.isNotEmpty()) {
                                addressParts.joinToString(", ")
                            } else {
                                "Posizione: ${location.latitude}, ${location.longitude}"
                            }
                        } else {
                            "Posizione: ${location.latitude}, ${location.longitude}"
                        }

                        binding.etAddressSearch.setText(addressText)
                        binding.tvSelectedAddress.text = "Indirizzo selezionato: $addressText"
                        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
                        mapFragment?.showLocation(latLng, addressText)
                        onAddressSelectedListener?.invoke(addressText, latLng)
                        onLocationReady(addressText, latLng)
                    } catch (e: Exception) {
                        // Fallback se il geocoding fallisce
                        val fallbackAddress = "Posizione: ${location.latitude}, ${location.longitude}"
                        binding.etAddressSearch.setText(fallbackAddress)
                        binding.tvSelectedAddress.text = "Indirizzo selezionato: $fallbackAddress"
                        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? MapFragment
                        mapFragment?.showLocation(latLng, fallbackAddress)
                        onAddressSelectedListener?.invoke(fallbackAddress, latLng)
                        onLocationReady(fallbackAddress, latLng)
                    }
                } else {
                    Toast.makeText(context, "Posizione non disponibile", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener { exception ->
                Toast.makeText(context, "Errore nel recupero della posizione: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Permesso posizione non concesso", Toast.LENGTH_SHORT).show()
        }
    }
}