package com.example.travelcompanion.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.DialogYearPickerBinding
import java.util.Calendar

class YearPickerDialog(
    private val maxYear: Int,
    private val minYear: Int,
    private val onYearSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val binding = DialogYearPickerBinding.inflate(requireActivity().layoutInflater)
        val numberPicker = binding.numberPickerYear.apply {
            this.minValue = minYear
            this.maxValue = maxYear
            this.value = Calendar.getInstance().get(Calendar.YEAR)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(binding.root)
            .create()

        binding.btnOkYear.setOnClickListener {
            onYearSelected(numberPicker.value)
            dialog.dismiss()
        }

        return dialog
    }
}
