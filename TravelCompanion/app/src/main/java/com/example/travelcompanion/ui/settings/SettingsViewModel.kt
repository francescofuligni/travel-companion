package com.example.travelcompanion.ui.settings

import android.net.Uri
import android.util.Log
import androidx.lifecycle.*
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.User
import com.example.travelcompanion.database.models.Image
import com.example.travelcompanion.repository.TravelRepository
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class SettingsViewModel(private val repository: TravelRepository) : ViewModel() {

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
                // Load user
                val user = repository.getUserById(1)
                _user.value = user

                // Load profile picture separately
                user?.profilePictureId?.let { imageId ->
                    val image = repository.getImageById(imageId)
                    image?.let { img ->
                        _profilePictureUri.value = Uri.parse(img.uri)
                    }
                }

                // Load home location
                user?.homeLocationId?.let { id ->
                    try {
                        val location = repository.getLocationById(id)
                        location?.let {
                            _homeLocation.value = LatLng(it.latitude, it.longitude)
                        }
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error loading location with id: $id", e)
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error loading user data", e)
                _message.value = "Error loading user data: ${e.message}"
            }
        }
    }

    fun saveUser(username: String, email: String, homeLocation: LatLng?, profilePictureUri: Uri? = null) {
        viewModelScope.launch {
            try {
                // Validate input data
                if (username.isBlank()) {
                    _message.value = "Username cannot be empty"
                    return@launch
                }
                
                if (email.isBlank()) {
                    _message.value = "Email cannot be empty"
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
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error saving location", e)
                        _message.value = "Error saving location: ${e.message}"
                        return@launch
                    }
                }

                // Handle profile picture
                var profilePictureId: Long? = null
                if (profilePictureUri != null) {
                    try {
                        val image = Image(
                            uri = profilePictureUri.toString(),
                            createdAt = System.currentTimeMillis()
                        )
                        profilePictureId = repository.insertImage(image)
                        Log.d("SettingsViewModel", "Saved profile picture with ID: $profilePictureId")
                    } catch (e: Exception) {
                        Log.e("SettingsViewModel", "Error saving profile picture", e)
                        _message.value = "Error saving profile picture: ${e.message}"
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
                        _message.value = "Error updating user: ${e.message}"
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
                        _message.value = "Error creating user: ${e.message}"
                        return@launch
                    }
                }
                
                _message.value = "Settings saved successfully"
                
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error saving user", e)
                _message.value = "Error saving settings: ${e.message}"
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
                _message.value = "App reset successfully"
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Error resetting app", e)
                _message.value = "Error resetting app: ${e.message}"
            }
        }
    }

    fun setHomeLocation(latLng: LatLng) {
        _homeLocation.value = latLng
    }
}