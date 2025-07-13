package com.example.travelcompanion.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Environment
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

object ProfilePictureUtils {
    
    private const val PROFILE_PICTURES_DIR = "profile_pictures"
    private const val MAX_IMAGE_SIZE = 1024
    
    /*
     * Creates a temporary image file for camera capture
     */
    fun createImageFile(context: Context): File? {
        return try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFileName = "JPEG_${timeStamp}_"
            val storageDir = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            File.createTempFile(imageFileName, ".jpg", storageDir)
        } catch (e: Exception) {
            Log.d("ProfilePictureUtils", e.printStackTrace().toString())
            null
        }
    }
    
    /*
     * Saves an image from URI to internal storage
     * Resizes the image if it exceeds maximum dimensions
     */
    fun saveImageToInternalStorage(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream: InputStream? = context.contentResolver.openInputStream(sourceUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()
            
            val resizedBitmap = resizeBitmap(bitmap)
            
            val profilePicturesDir = File(context.filesDir, PROFILE_PICTURES_DIR)
            if (!profilePicturesDir.exists()) {
                profilePicturesDir.mkdirs()
            }
            
            val fileName = "profile_${System.currentTimeMillis()}.jpg"
            val file = File(profilePicturesDir, fileName)
            
            val outputStream = FileOutputStream(file)
            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
            outputStream.close()
            
            Uri.fromFile(file)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /*
     * Resizes bitmap to fit within maximum dimensions while maintaining aspect ratio
     */
    private fun resizeBitmap(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= MAX_IMAGE_SIZE && height <= MAX_IMAGE_SIZE) {
            return bitmap
        }
        
        val ratio = minOf(MAX_IMAGE_SIZE.toFloat() / width, MAX_IMAGE_SIZE.toFloat() / height)
        val newWidth = (width * ratio).toInt()
        val newHeight = (height * ratio).toInt()
        
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }
}