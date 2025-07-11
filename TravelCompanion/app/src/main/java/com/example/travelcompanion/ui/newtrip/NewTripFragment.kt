package com.example.travelcompanion.ui.newtrip

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment

import androidx.navigation.fragment.findNavController
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentNewTripBinding
import com.example.travelcompanion.ui.home.NewTripViewModel
import com.example.travelcompanion.ui.home.NewTripVMFactory
import com.example.travelcompanion.database.TravelDatabase
import com.example.travelcompanion.repository.TravelRepository
import java.util.Calendar

class NewTripFragment : Fragment() {

    private var _binding: FragmentNewTripBinding? = null
    private val binding get() = _binding!!

    private lateinit var viewModel: NewTripViewModel

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNewTripBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val database = TravelDatabase.getDatabase(requireContext())
        val repository = TravelRepository(
            database.userDao(),
            database.locationDao(),
            database.tripDao(),
            database.imageDao()
        )
        val factory = NewTripVMFactory(repository, requireContext())
        viewModel = ViewModelProvider(this, factory)[NewTripViewModel::class.java]

        // Imposta la data minima a oggi e mantiene la selezione di default
        val datePicker = binding.datePickerEnd
        datePicker.minDate = System.currentTimeMillis()

        // Gestione abilitazione/disabilitazione campi in base al tipo di viaggio
        val toggleGroup = binding.toggleTripType
        val today = Calendar.getInstance()
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (!isChecked) return@addOnButtonCheckedListener
            when (checkedId) {
                R.id.btnLocal -> {
                    binding.etDestination.isEnabled = false
                    binding.datePickerEnd.isEnabled = false
                    binding.datePickerEnd.updateDate(
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH)
                    )
                }
                R.id.btnOneDay -> {
                    binding.etDestination.isEnabled = true
                    binding.datePickerEnd.isEnabled = false
                    binding.datePickerEnd.updateDate(
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH)
                    )
                }
                R.id.btnMultiDays -> {
                    binding.etDestination.isEnabled = true
                    binding.datePickerEnd.isEnabled = true
                }
            }
        }

        binding.btnStartTrip.isEnabled = false

        binding.etTripTitle.doOnTextChanged { _, _, _, _ -> updateBtnStartTripState() }
        binding.etDestination.doOnTextChanged { _, _, _, _ -> updateBtnStartTripState() }

        toggleGroup.addOnButtonCheckedListener { _, _, _ ->
            updateBtnStartTripState()
        }

        binding.btnStartTrip.setOnClickListener {
            val title = binding.etTripTitle.text.toString()
            val destination = binding.etDestination.text.toString()
            val type = when (binding.toggleTripType.checkedButtonId) {
                R.id.btnLocal -> "local"
                R.id.btnOneDay -> "one_day"
                R.id.btnMultiDays -> "multi_days"
                else -> "unknown"
            }

            val calendar = Calendar.getInstance()
            calendar.set(binding.datePickerEnd.year, binding.datePickerEnd.month, binding.datePickerEnd.dayOfMonth)
            val endDate = calendar.time

            viewModel.startTrip(title, destination, type, endDate)

            findNavController().navigate(R.id.action_nav_new_trip_to_nav_home)
        }
    }

    private fun updateBtnStartTripState() {
        val titleNotEmpty = binding.etTripTitle.text.toString().isNotBlank()
        val selectedType = binding.toggleTripType.checkedButtonId
        val destinationRequired = selectedType != R.id.btnLocal
        val destinationNotEmpty = if (destinationRequired) {
            binding.etDestination.text.toString().isNotBlank()
        } else true

        binding.btnStartTrip.isEnabled = titleNotEmpty
            && destinationNotEmpty
            && selectedType != View.NO_ID
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}