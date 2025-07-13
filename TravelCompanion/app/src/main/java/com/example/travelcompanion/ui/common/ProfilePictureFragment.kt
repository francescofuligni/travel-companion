package com.example.travelcompanion.ui.common

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.example.travelcompanion.R
import com.example.travelcompanion.databinding.FragmentProfilePicturePickerBinding
import com.example.travelcompanion.utils.ProfilePictureUtils

/**
 * Fragment per la selezione e gestione della foto profilo
 * Permette di scattare una foto o selezionare un'immagine dalla galleria
 */
class ProfilePictureFragment : Fragment() {
    
    private var _binding: FragmentProfilePicturePickerBinding? = null
    private val binding get() = _binding!!
    
    private var currentPhotoUri: Uri? = null
    private var onImageSelectedListener: ((Uri?) -> Unit)? = null
    
    /**
     * Launcher per la selezione dalla galleria
     */
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }
    
    /**
     * Launcher per la cattura dalla fotocamera
     */
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }
    
    /**
     * Launcher per la richiesta permesso fotocamera
     */
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), getString(R.string.camera_permission_required), Toast.LENGTH_SHORT).show()
        }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfilePicturePickerBinding.inflate(inflater, container, false)
        return binding.root
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }
    
    /**
     * Configura i listener per i pulsanti
     */
    private fun setupViews() {
        binding.btnSelectFromGallery.setOnClickListener {
            openGallery()
        }
        
        binding.btnTakePhoto.setOnClickListener {
            checkCameraPermissionAndOpen()
        }
        
        binding.btnRemovePhoto.setOnClickListener {
            removePhoto()
        }
    }
    
    /**
     * Apre la galleria per selezionare un'immagine
     */
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    
    /**
     * Controlla i permessi e apre la fotocamera
     */
    private fun checkCameraPermissionAndOpen() {
        when {
            ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED -> {
                openCamera()
            }
            else -> {
                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        }
    }
    
    /**
     * Apre la fotocamera per scattare una foto
     */
    private fun openCamera() {
        val photoFile = ProfilePictureUtils.createImageFile(requireContext())
        photoFile?.let { file ->
            currentPhotoUri = FileProvider.getUriForFile(
                requireContext(),
                "${requireContext().packageName}.fileprovider",
                file
            )
            
            val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                putExtra(MediaStore.EXTRA_OUTPUT, currentPhotoUri)
            }
            cameraLauncher.launch(intent)
        }
    }
    
    /**
     * Gestisce la selezione di un'immagine
     */
    private fun handleImageSelection(uri: Uri) {
        try {
            val savedUri = ProfilePictureUtils.saveImageToInternalStorage(requireContext(), uri)
            displayImage(savedUri)
            onImageSelectedListener?.invoke(savedUri)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), getString(R.string.error_saving_image, e.message), Toast.LENGTH_SHORT).show()
        }
    }
    
    /**
     * Mostra l'immagine selezionata nell'interfaccia
     */
    private fun displayImage(uri: Uri?) {
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_myplaces)
                .into(binding.ivProfilePicture)
            
            binding.btnRemovePhoto.visibility = View.VISIBLE
        } else {
            binding.ivProfilePicture.setImageResource(android.R.drawable.ic_menu_myplaces)
            binding.btnRemovePhoto.visibility = View.GONE
        }
    }
    
    /**
     * Rimuove la foto profilo
     */
    private fun removePhoto() {
        displayImage(null)
        onImageSelectedListener?.invoke(null)
    }
    
    /**
     * Imposta l'immagine corrente da mostrare
     */
    fun setCurrentImage(uri: Uri?) {
        displayImage(uri)
    }
    
    /**
     * Imposta il listener per la selezione dell'immagine
     */
    fun setOnImageSelectedListener(listener: (Uri?) -> Unit) {
        onImageSelectedListener = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}