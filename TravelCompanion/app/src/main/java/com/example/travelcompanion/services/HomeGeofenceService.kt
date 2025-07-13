package com.example.travelcompanion.services

import kotlinx.coroutines.launch
import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.travelcompanion.utils.NotificationUtils
import com.example.travelcompanion.repository.TravelRepository
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

object HomeGeofenceService {

    private const val GEOFENCE_ID = "HOME_GEOFENCE_ID"
    private const val GEOFENCE_RADIUS_METERS = 1000f

    fun registerHomeGeofence(context: Context, latitude: Double, longitude: Double) {
        val geofencingClient = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .build()

        val geofencingRequest = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_EXIT)
            .addGeofence(geofence)
            .build()

        val pendingIntent = getGeofencePendingIntent(context)

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permesso non concesso.
            return
        }
        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener { Log.d("Geofence", "Home geofence added") }
            .addOnFailureListener { Log.e("Geofence", "Failed to add geofence", it) }
    }

    private fun getGeofencePendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, HomeGeofenceReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class HomeGeofenceReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val repo = TravelRepository.create(context)

        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val isTripActive = repo.getActiveTripId() != null

            if (!isTripActive) {
                NotificationUtils.sendNotification(
                    context = context,
                    channelId = "geofence_channel",
                    channelName = "Promemoria viaggi",
                    title = "Buon viaggio!",
                    message = "Sei uscito da casa: ricorda di avviare il tuo viaggio.",
                    notificationId = 2,
                    iconRes = android.R.drawable.ic_dialog_info
                )
            }
        }
    }
}