package com.example.travelcompanion.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.example.travelcompanion.databinding.FragmentHomeBinding
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.navigation.fragment.findNavController
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.example.travelcompanion.R
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import android.content.pm.PackageManager
import android.content.ActivityNotFoundException
import android.os.SystemClock

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var photoFile: File

    private val cameraLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                Toast.makeText(requireContext(), "Foto salvata!", Toast.LENGTH_SHORT).show()
                // Qui puoi gestire la foto salvata in photoFile
            } else {
                Toast.makeText(requireContext(), "Foto non scattata.", Toast.LENGTH_SHORT).show()
            }
        }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "JPEG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Avvia il cronometro all'apertura del fragment
        binding.chronometer.base = SystemClock.elapsedRealtime()
        binding.chronometer.start()

        binding.btnStop.setOnClickListener {
            // Ferma il cronometro quando si preme "Interrompi"
            binding.chronometer.stop()
        }

        binding.btnNote.setOnClickListener {
            findNavController().navigate(R.id.action_nav_home_to_nav_note)
        }

        binding.btnPhoto.setOnClickListener {
            val pm = requireContext().packageManager
            // Verifica presenza hardware fotocamera
            if (!pm.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                Toast.makeText(requireContext(),
                    "Nessuna fotocamera disponibile sul dispositivo.",
                    Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            try {
                // Crea il file per la foto
                photoFile = createImageFile()
                val photoUri = FileProvider.getUriForFile(
                    requireContext(),
                    "${requireContext().packageName}.fileprovider",
                    photoFile
                )

                // Prepara e lancia l’Intent fotocamera
                val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                    putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    addFlags(
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                cameraLauncher.launch(intent)

            } catch (ex: IOException) {
                Toast.makeText(requireContext(),
                    "Errore durante la creazione del file per la foto.",
                    Toast.LENGTH_SHORT).show()
            } catch (ex: ActivityNotFoundException) {
                Toast.makeText(requireContext(),
                    "Nessuna app fotocamera trovata.",
                    Toast.LENGTH_SHORT).show()
            }
        }

        // Inserisce il MapFragment riutilizzabile
        childFragmentManager.beginTransaction()
            .replace(binding.mapContainer.id, MapFragment())
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}