package com.example.travelcompanion.ui.settings

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.services.HomeGeofenceService
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application, private val repository: TravelRepository) : AndroidViewModel(application) {

    private fun postMessage(msg: String) {
        _message.value = msg
    }

    private val appContext = application.applicationContext

    private val _user = MutableLiveData<User?>()
    val user: LiveData<User?> = _user

    private val _homeLocation = MutableLiveData<LatLng?>()
    val homeLocation: LiveData<LatLng?> = _homeLocation

    private val _message = MutableLiveData<String>()
    val message: LiveData<String> = _message

    private val _profilePictureUri = MutableLiveData<Uri?>()
    val profilePictureUri: LiveData<Uri?> = _profilePictureUri

    fun loadUserData() {
        viewModelScope.launch {
            try {
                loadUser()
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading user data", e)
                postMessage("Error loading user data: ${e.message}")
            }
        }
    }

    private suspend fun loadUser() {
        val user = repository.getUserById(1)
        _user.value = user
        user?.let {
            loadProfilePicture(it)
            loadHomeLocation(it)
        }
    }

    private suspend fun loadProfilePicture(user: User) {
        user.profilePictureId?.let { id ->
            repository.getImageById(id)?.let { img ->
                _profilePictureUri.value = Uri.parse(img.uri)
            }
        }
    }

    private suspend fun loadHomeLocation(user: User) {
        user.homeLocationId?.let { id ->
            try {
                repository.getLocationById(id)?.let {
                    _homeLocation.value = LatLng(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading location with id: $id", e)
            }
        }
    }

    fun saveUser(username: String, email: String, homeLocation: LatLng?, profilePictureUri: Uri? = null) {
        viewModelScope.launch {
            try {
                // Validate input data
                if (username.isBlank()) {
                    postMessage("Username cannot be empty")
                    return@launch
                }
                
                if (email.isBlank()) {
                    postMessage("Email cannot be empty")
                    return@launch
                }

                // Handle home location
                var homeLocationId: Long? = null
                if (homeLocation != null) {
                    try {
                        val location = Location(
                            latitude = homeLocation.latitude,
                            longitude = homeLocation.longitude
                        )
                        homeLocationId = repository.insertLocation(location)
                        Log.d("SettingsViewModel", "Saved location with ID: $homeLocationId")
                        HomeGeofenceService.registerHomeGeofence(
                            context = appContext,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error saving location", e)
                        postMessage("Error saving location: ${e.message}")
                        return@launch
                    }
                }

                // Handle profile picture
                var profilePictureId: Long? = null
                if (profilePictureUri != null) {
                    try {
                        val image = Image(
                            tripId = null, // Profile picture not associated with a trip
                            uri = profilePictureUri.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                        profilePictureId = repository.insertImage(image)
                        Log.d("SettingsViewModel", "Saved profile picture with ID: $profilePictureId")
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error saving profile picture", e)
                        postMessage("Error saving profile picture: ${e.message}")
                        return@launch
                    }
                }

                // Get current user
                val currentUser = repository.getUserById(1)

                if (currentUser != null) {
                    // Update existing user
                    val updatedUser = currentUser.copy(
                        name = username,
                        email = email,
                        homeLocationId = homeLocationId,
                        profilePictureId = profilePictureId ?: currentUser.profilePictureId
                    )
                    
                    try {
                        repository.updateUser(updatedUser)
                        _user.value = updatedUser
                        Log.d("SettingsViewModel", "Updated user: $updatedUser")
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error updating user", e)
                        postMessage("Error updating user: ${e.message}")
                        return@launch
                    }
                } else {
                    // Create new user
                    val newUser = User(
                        id = 1,
                        name = username,
                        email = email,
                        homeLocationId = homeLocationId,
                        profilePictureId = profilePictureId
                    )
                    
                    try {
                        repository.insertUser(newUser)
                        _user.value = newUser
                        Log.d("SettingsViewModel", "Created new user: $newUser")
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error creating user", e)
                        postMessage("Error creating user: ${e.message}")
                        return@launch
                    }
                }
                
                postMessage("Settings saved successfully")
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error saving user", e)
                postMessage("Error saving settings: ${e.message}")
            }
        }
    }

    fun updateProfilePicture(uri: Uri?) {
        _profilePictureUri.value = uri
    }

    fun resetApp() {
        viewModelScope.launch {
            try {
                repository.deleteAllUsers()
                repository.deleteAllLocations()
                repository.deleteAllTrips()
                // Clear all images
                val images = repository.imageDao.getAllImages()
                images.forEach { image -> repository.deleteImage(image) }
                
                _user.value = null
                _homeLocation.value = null
                _profilePictureUri.value = null
                postMessage("App reset successfully")
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error resetting app", e)
                postMessage("Error resetting app: ${e.message}")
            }
        }
    }

    fun setHomeLocation(latLng: LatLng) {
        _homeLocation.value = latLng
    }
}