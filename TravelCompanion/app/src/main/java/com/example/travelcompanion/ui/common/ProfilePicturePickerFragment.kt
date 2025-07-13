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
import com.example.travelcompanion.databinding.FragmentProfilePicturePickerBinding
import com.example.travelcompanion.utils.ProfilePictureUtils

class ProfilePicturePickerFragment : Fragment() {
    
    private var _binding: FragmentProfilePicturePickerBinding? = null
    private val binding get() = _binding!!
    
    private var currentPhotoUri: Uri? = null
    private var onImageSelectedListener: ((Uri?) -> Unit)? = null
    
    // Activity result launchers
    private val galleryLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }
    
    private val cameraLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            currentPhotoUri?.let { uri ->
                handleImageSelection(uri)
            }
        }
    }
    
    private val cameraPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            openCamera()
        } else {
            Toast.makeText(requireContext(), "Camera permission is required", Toast.LENGTH_SHORT).show()
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
    
    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        galleryLauncher.launch(intent)
    }
    
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
    
    private fun handleImageSelection(uri: Uri) {
        try {
            // Save the image to internal storage
            val savedUri = ProfilePictureUtils.saveImageToInternalStorage(requireContext(), uri)
            
            // Update UI
            displayImage(savedUri)
            
            // Notify listener
            onImageSelectedListener?.invoke(savedUri)
            
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error saving image: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
    
    private fun displayImage(uri: Uri?) {
        if (uri != null) {
            Glide.with(this)
                .load(uri)
                .circleCrop()
                .placeholder(android.R.drawable.ic_menu_myplaces) // Use Android built-in resource
                .into(binding.ivProfilePicture)
            
            binding.btnRemovePhoto.visibility = View.VISIBLE
        } else {
            binding.ivProfilePicture.setImageResource(android.R.drawable.ic_menu_myplaces) // Use Android built-in resource
            binding.btnRemovePhoto.visibility = View.GONE
        }
    }
    
    private fun removePhoto() {
        displayImage(null)
        onImageSelectedListener?.invoke(null)
    }
    
    fun setCurrentImage(uri: Uri?) {
        displayImage(uri)
    }
    
    fun setOnImageSelectedListener(listener: (Uri?) -> Unit) {
        onImageSelectedListener = listener
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}