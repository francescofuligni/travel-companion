package com.example.travelcompanion.ui.common

import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import android.widget.NumberPicker
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.example.travelcompanion.R
import java.util.Calendar

class YearPickerDialog(
    private val maxYear: Int,
    private val minYear: Int,
    private val onYearSelected: (Int) -> Unit
) : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val inflater = requireActivity().layoutInflater
        val view = inflater.inflate(R.layout.dialog_year_picker, null)
        val numberPicker = view.findViewById<NumberPicker>(R.id.numberPickerYear).apply {
            this.minValue = minYear
            this.maxValue = maxYear
            this.value = Calendar.getInstance().get(Calendar.YEAR)
        }

        val dialog = AlertDialog.Builder(requireContext())
            .setView(view)
            .create()

        view.findViewById<Button>(R.id.btnOkYear).setOnClickListener {
            onYearSelected(numberPicker.value)
            dialog.dismiss()
        }

        return dialog
    }
}
