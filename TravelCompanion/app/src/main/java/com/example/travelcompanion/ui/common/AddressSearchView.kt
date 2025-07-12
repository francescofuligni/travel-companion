package com.example.travelcompanion.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.LinearLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.example.travelcompanion.BuildConfig
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.ViewAddressSearchBinding
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

class AddressSearchView @JvmOverloads constructor(
    context: Context, 
    attrs: AttributeSet? = null, 
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val binding: ViewAddressSearchBinding
    private var onAddressSelectedListener: ((String, LatLng) -> Unit)? = null

    init {
        binding = ViewAddressSearchBinding.inflate(LayoutInflater.from(context), this, true)
        setupViews()
    }

    private fun setupViews() {
        binding.btnSearchAddress.setOnClickListener {
            val address = binding.etAddressSearch.text.toString().trim()
            if (address.isNotEmpty()) {
                searchAddress(address)
            } else {
                Toast.makeText(context, "Inserisci un indirizzo", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun setOnAddressSelectedListener(listener: (String, LatLng) -> Unit) {
        onAddressSelectedListener = listener
    }

    private fun searchAddress(address: String) {
        if (context is androidx.fragment.app.FragmentActivity) {
            (context as androidx.fragment.app.FragmentActivity).lifecycleScope.launch {
                val latLng = geocodeAddress(address)
                if (latLng != null) {
                    onAddressSelectedListener?.invoke(address, latLng)
                    binding.tvSelectedAddress.text = "Indirizzo selezionato: $address"
                } else {
                    Toast.makeText(context, "Indirizzo non trovato", Toast.LENGTH_SHORT).show()
                }
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

    fun setSelectedAddress(address: String) {
        binding.etAddressSearch.setText(address)
        binding.tvSelectedAddress.text = "Indirizzo selezionato: $address"
    }

    fun clearSelection() {
        binding.etAddressSearch.setText("")
        binding.tvSelectedAddress.text = "Nessun indirizzo selezionato"
    }
}
