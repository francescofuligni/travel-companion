package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.R

import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import java.text.SimpleDateFormat
import java.util.*

class HomeActiveTripFragment : Fragment() {

    private var tripId: Long = -1L
    private lateinit var viewModel: HomeActiveTripViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tripId = arguments?.getLong("tripId") ?: -1L
        val repository = TravelRepository.create(requireContext())
        val factory = HomeActiveTripVMFactory(requireActivity().application, repository)
        viewModel = ViewModelProvider(this, factory)[HomeActiveTripViewModel::class.java]
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home_active_trip, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val tvStartDate = view.findViewById<TextView>(R.id.tv_start_date)
        val tvEndDate = view.findViewById<TextView>(R.id.tv_end_date)
        val tvDistance = view.findViewById<TextView>(R.id.tv_distance)
        val tvDuration = view.findViewById<TextView>(R.id.tv_duration_label)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        viewModel.getTripById(tripId).observe(viewLifecycleOwner, Observer { trip ->
            tvStartDate.text = dateFormat.format(Date(trip.startDate))
            tvEndDate.text = dateFormat.format(Date(trip.endDate))
            tvDistance.text = "Distanza: %.2f km".format(trip.distance)
            tvDuration.text = "Durata: %.0f sec".format(trip.duration)
        })

        val stopTripButton = view.findViewById<View>(R.id.btn_stop)
        stopTripButton.setOnClickListener {
            viewModel.endTrip(tripId) {
                Toast.makeText(requireContext(), "Viaggio completato!", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.nav_my_trips)
            }
        }
    }
}
