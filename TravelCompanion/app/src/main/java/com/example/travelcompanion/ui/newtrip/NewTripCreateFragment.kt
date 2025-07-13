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
import com.example.travelcompanion.ui.common.AddressSearchFragment
import com.google.android.gms.maps.model.LatLng
import java.util.Calendar
import android.text.InputFilter
import android.widget.Toast
import android.app.Application

class NewTripCreateFragment : Fragment() {

    private var selectedAddressText: String? = null

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
        // Limite massimo di 100 caratteri per il titolo del viaggio
        binding.etTripTitle.filters = arrayOf(InputFilter.LengthFilter(100))
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

        val factory = NewTripCreateViewModelFactory(requireActivity().application)
        viewModel = ViewModelProvider(this, factory)[NewTripCreateViewModel::class.java]

        val datePicker = binding.datePickerEnd
        datePicker.minDate = System.currentTimeMillis()

        val toggleGroup = binding.toggleTripType
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            val today = Calendar.getInstance()
            val addressSearchFragment = childFragmentManager.findFragmentById(R.id.addressSearchContainer) as? AddressSearchFragment

            when (checkedId) {
                binding.btnLocal.id -> {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        addressSearchFragment?.setInputEnabled(false)
                        addressSearchFragment?.getUserLocation { address, latLng ->
                            selectedDestinationLatLng = latLng
                            selectedAddressText = address
                            addressSearchFragment.setAddressText(address)
                            updateBtnStartTripState()
                        }
                    }
                    binding.datePickerEnd.isEnabled = false
                    binding.datePickerEnd.updateDate(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH))
                    binding.datePickerEnd.minDate = today.timeInMillis
                }
                binding.btnOneDay.id -> {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        addressSearchFragment?.setInputEnabled(true)
                    }

                    val todayDate = Calendar.getInstance()
                    binding.datePickerEnd.isEnabled = false
                    binding.datePickerEnd.minDate = todayDate.timeInMillis
                    binding.datePickerEnd.updateDate(
                        todayDate.get(Calendar.YEAR),
                        todayDate.get(Calendar.MONTH),
                        todayDate.get(Calendar.DAY_OF_MONTH)
                    )
                }
                binding.btnMultiDays.id -> {
                    viewLifecycleOwner.lifecycleScope.launchWhenResumed {
                        addressSearchFragment?.setInputEnabled(true)
                    }
                    binding.datePickerEnd.isEnabled = true

                    val tomorrow = Calendar.getInstance().apply { add(Calendar.DAY_OF_MONTH, 1) }
                    binding.datePickerEnd.minDate = tomorrow.timeInMillis
                    binding.datePickerEnd.updateDate(tomorrow.get(Calendar.YEAR), tomorrow.get(Calendar.MONTH), tomorrow.get(Calendar.DAY_OF_MONTH))
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launchWhenResumed {
            fragment.setOnAddressSelectedListener { address, latLng ->
                selectedDestinationLatLng = latLng
                selectedAddressText = address
                updateBtnStartTripState()
            }
        }

        binding.btnStartTrip.isEnabled = false

        binding.etTripTitle.doOnTextChanged { _, _, _, _ -> updateBtnStartTripState() }

        toggleGroup.addOnButtonCheckedListener { _, _, _ -> updateBtnStartTripState() }

        binding.btnStartTrip.setOnClickListener {
            val rawTitle = binding.etTripTitle.text.toString()
            if (rawTitle.length > 100) {
                Toast.makeText(requireContext(), getString(R.string.error_too_long), Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            val title = rawTitle.trim()
            val destination = selectedAddressText ?: ""
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
        val addressSelected = selectedDestinationLatLng != null

        val dataValida = binding.datePickerEnd.year >= Calendar.getInstance().get(Calendar.YEAR) // semplice validazione

        binding.btnStartTrip.isEnabled =
            titleNotEmpty && selectedType != View.NO_ID && addressSelected && dataValida
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