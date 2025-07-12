package com.example.travelcompanion.ui.newtrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentNewTripCreateBinding
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.ui.common.AddressSearchFragment
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar
import android.widget.Toast

class NewTripCreateFragment : Fragment() {

    private lateinit var foregroundServiceLocationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private lateinit var fineLocationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>
    private var pendingTripData: PendingTripData? = null

    data class PendingTripData(
        val title: String,
        val destination: String,
        val type: String,
        val endDate: java.util.Date,
        val selectedDestinationLatLng: LatLng?
    )

    private var _binding: FragmentNewTripCreateBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NewTripCreateViewModel
    private var selectedDestinationLatLng: LatLng? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTripCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fragment = AddressSearchFragment()
        childFragmentManager.beginTransaction()
            .replace(R.id.addressSearchContainer, fragment)
            .commit()

        foregroundServiceLocationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pendingTripData?.let { requestFineLocationPermissionAndStartTrip(it) }
            } else {
                Toast.makeText(requireContext(), "Permesso FOREGROUND_SERVICE_LOCATION negato", Toast.LENGTH_LONG).show()
                pendingTripData = null
            }
        }

        fineLocationPermissionLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
        ) { isGranted ->
            if (isGranted) {
                pendingTripData?.let {
                    viewModel.startTrip(it.title, it.destination, it.type, it.endDate, it.selectedDestinationLatLng)
                    findNavController().navigate(R.id.nav_home)
                }
            } else {
                Toast.makeText(requireContext(), "Permesso ACCESS_FINE_LOCATION negato", Toast.LENGTH_LONG).show()
            }
            pendingTripData = null
        }

        val repository = TravelRepository.create(requireContext())
        val factory = NewTripCreateViewModelFactory(repository, requireContext())
        viewModel = ViewModelProvider(this, factory)[NewTripCreateViewModel::class.java]

        val datePicker = binding.datePickerEnd
        datePicker.minDate = System.currentTimeMillis()

        val toggleGroup = binding.toggleTripType
        val today = Calendar.getInstance()
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                binding.btnLocal.id -> {
                    binding.etDestination.isEnabled = true
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        fragment.setInputEnabled(true)
                    }
                    binding.datePickerEnd.isEnabled = false
                    datePicker.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                }
                binding.btnOneDay.id -> {
                    binding.etDestination.isEnabled = false
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        fragment.setInputEnabled(false)
                    }
                    binding.datePickerEnd.isEnabled = false
                    datePicker.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    // Imposta valore simulato (sostituire con localizzazione reale)
                    binding.etDestination.setText("Posizione corrente")
                }
                binding.btnMultiDays.id -> {
                    binding.etDestination.isEnabled = true
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        fragment.setInputEnabled(true)
                    }
                    binding.datePickerEnd.isEnabled = true
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            fragment.setOnAddressSelectedListener { address, latLng ->
                binding.etDestination.setText(address)
                selectedDestinationLatLng = latLng
                updateBtnStartTripState()
            }
        }

        binding.btnStartTrip.isEnabled = false

        binding.etTripTitle.doOnTextChanged { _, _, _, _ -> updateBtnStartTripState() }
        binding.etDestination.doOnTextChanged { _, _, _, _ -> updateBtnStartTripState() }

        toggleGroup.addOnButtonCheckedListener { _, _, _ -> updateBtnStartTripState() }

        binding.btnStartTrip.setOnClickListener {
            val title = binding.etTripTitle.text.toString()
            val destination = binding.etDestination.text.toString()
            val type = when (binding.toggleTripType.checkedButtonId) {
                binding.btnLocal.id -> "local"
                binding.btnOneDay.id -> "one_day"
                binding.btnMultiDays.id -> "multi_days"
                else -> "unknown"
            }
            val calendar = Calendar.getInstance()
            calendar.set(binding.datePickerEnd.year, binding.datePickerEnd.month, binding.datePickerEnd.dayOfMonth)
            val endDate = calendar.time
            val tripData = PendingTripData(title, destination, type, endDate, selectedDestinationLatLng)
            checkAndRequestPermissionsThenStartTrip(tripData)
        }
    }

    private fun updateBtnStartTripState() {
        val titleNotEmpty = binding.etTripTitle.text.toString().isNotBlank()
        val selectedType = binding.toggleTripType.checkedButtonId
        val destinationRequired = selectedType != binding.btnLocal.id
        val destinationNotEmpty = if (destinationRequired) {
            binding.etDestination.text.toString().isNotBlank()
        } else true

        binding.btnStartTrip.isEnabled = titleNotEmpty && destinationNotEmpty && selectedType != View.NO_ID
    }

    private fun checkAndRequestPermissionsThenStartTrip(tripData: PendingTripData) {
        val foregroundServiceLocationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.FOREGROUND_SERVICE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        val fineLocationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (foregroundServiceLocationGranted && fineLocationGranted) {
            viewModel.startTrip(tripData.title, tripData.destination, tripData.type, tripData.endDate, tripData.selectedDestinationLatLng)
            findNavController().navigate(R.id.nav_home)
        } else {
            pendingTripData = tripData
            if (!foregroundServiceLocationGranted) {
                foregroundServiceLocationPermissionLauncher.launch(android.Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            } else {
                requestFineLocationPermissionAndStartTrip(tripData)
            }
        }
    }

    private fun requestFineLocationPermissionAndStartTrip(tripData: PendingTripData) {
        val fineLocationGranted = androidx.core.content.ContextCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (fineLocationGranted) {
            viewModel.startTrip(tripData.title, tripData.destination, tripData.type, tripData.endDate, tripData.selectedDestinationLatLng)
            findNavController().navigate(R.id.nav_home)
            pendingTripData = null
        } else {
            fineLocationPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}