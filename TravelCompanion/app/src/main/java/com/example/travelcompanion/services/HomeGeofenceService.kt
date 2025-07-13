package com.example.travelcompanion.services

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
import com.google.android.gms.location.GeofencingEvent
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Servizio per la gestione del geofencing della casa dell'utente
 */
object HomeGeofenceService {

    private const val GEOFENCE_ID = "HOME_GEOFENCE_ID"
    private const val GEOFENCE_RADIUS_METERS = 500f // Aumentato per test in emulatore
    private const val TAG = "HomeGeofenceService"

    /**
     * Registra il geofence per la casa dell'utente
     */
    fun registerHomeGeofence(context: Context, latitude: Double, longitude: Double) {
        Log.d(TAG, "Registrazione geofence casa: lat=$latitude, lon=$longitude, radius=$GEOFENCE_RADIUS_METERS")

        // 1. Check if location is enabled
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as android.location.LocationManager
        val isLocationEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)
        Log.d(TAG, "Location enabled: $isLocationEnabled")
        if (!isLocationEnabled) {
            Log.e(TAG, "Location disattivata: impossibile registrare geofence")
            NotificationUtils.sendNotification(
                context = context,
                channelId = "geofence_channel",
                channelName = "Geofence Error",
                title = "Errore geofence",
                message = "Attiva la posizione per abilitare il promemoria di uscita da casa.",
                notificationId = 998,
                iconRes = android.R.drawable.ic_dialog_alert,
                channelDescription = "Errore geofence location spenta"
            )
            return
        }

        // 2. Check Google Play Services availability
        val playServicesAvailable = try {
            val gms = com.google.android.gms.common.GoogleApiAvailability.getInstance()
            val resultCode = gms.isGooglePlayServicesAvailable(context)
            resultCode == com.google.android.gms.common.ConnectionResult.SUCCESS
        } catch (e: Exception) {
            Log.e(TAG, "Errore nel controllo Google Play Services", e)
            false
        }
        Log.d(TAG, "Google Play Services available: $playServicesAvailable")
        if (!playServicesAvailable) {
            Log.e(TAG, "Google Play Services non disponibile o non aggiornato")
            NotificationUtils.sendNotification(
                context = context,
                channelId = "geofence_channel",
                channelName = "Geofence Error",
                title = "Errore geofence",
                message = "Google Play Services non disponibile o non aggiornato. Aggiorna Google Play Services e riprova.",
                notificationId = 995,
                iconRes = android.R.drawable.ic_dialog_alert,
                channelDescription = "Errore geofence play services"
            )
            return
        }

        val geofencingClient = LocationServices.getGeofencingClient(context)

        val geofence = Geofence.Builder()
            .setRequestId(GEOFENCE_ID)
            .setCircularRegion(latitude, longitude, GEOFENCE_RADIUS_METERS)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_EXIT or Geofence.GEOFENCE_TRANSITION_ENTER)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setLoiteringDelay(10000) // 10 secondi
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
            Log.e(TAG, "Permessi di localizzazione non concessi")
            NotificationUtils.sendNotification(
                context = context,
                channelId = "geofence_channel",
                channelName = "Geofence Error",
                title = "Errore geofence",
                message = "Permessi di localizzazione non concessi. Abilita i permessi per la posizione.",
                notificationId = 997,
                iconRes = android.R.drawable.ic_dialog_alert,
                channelDescription = "Errore geofence permessi"
            )
            return
        }

        Log.d(TAG, "Permessi OK, provo a registrare geofence con lat=$latitude, lon=$longitude, radius=$GEOFENCE_RADIUS_METERS")

        geofencingClient.addGeofences(geofencingRequest, pendingIntent)
            .addOnSuccessListener {
                Log.d(TAG, "Home geofence registrato con successo")
                NotificationUtils.sendNotification(
                    context = context,
                    channelId = "geofence_channel",
                    channelName = "Geofence Test",
                    title = "Geofence Registrato",
                    message = "Il geofence della casa è stato registrato con successo",
                    notificationId = 999,
                    iconRes = android.R.drawable.ic_dialog_info,
                    channelDescription = "Test geofence registration"
                )
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Errore registrazione geofence: ${exception.message}")
                Log.e(TAG, "Exception details: ", exception)
                // Try to get error code if possible
                val errorCode = if (exception is com.google.android.gms.common.api.ApiException) exception.statusCode else null
                val errorMsg = when (errorCode) {
                    1000 -> "GEOFENCE_NOT_AVAILABLE: Servizi di localizzazione non disponibili o Google Play Services non aggiornato."
                    1001 -> "GEOFENCE_TOO_MANY_GEOFENCES: Limite massimo di geofence raggiunto."
                    1002 -> "GEOFENCE_TOO_MANY_PENDING_INTENTS: Troppi PendingIntent registrati."
                    else -> "Errore sconosciuto."
                }
                NotificationUtils.sendNotification(
                    context = context,
                    channelId = "geofence_channel",
                    channelName = "Geofence Error",
                    title = "Errore geofence",
                    message = "Errore durante la registrazione del geofence: ${exception.message}\n$errorMsg",
                    notificationId = 996,
                    iconRes = android.R.drawable.ic_dialog_alert,
                    channelDescription = "Errore geofence generico"
                )
            }
    }

    /**
     * Rimuove il geofence della casa
     */
    fun removeHomeGeofence(context: Context) {
        Log.d(TAG, "Rimozione geofence casa")
        val geofencingClient = LocationServices.getGeofencingClient(context)
        geofencingClient.removeGeofences(listOf(GEOFENCE_ID))
            .addOnSuccessListener { Log.d(TAG, "Home geofence rimosso") }
            .addOnFailureListener { Log.e(TAG, "Errore rimozione geofence", it) }
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

/**
 * Receiver per gli eventi di geofencing
 */
class HomeGeofenceReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent?.hasError() == true) {
            return
        }

        // Notifica per evento geofence
        NotificationUtils.sendNotification(
            context = context,
            channelId = "geofence_channel",
            channelName = "Geofence Event",
            title = "Sei passato da casa?",
            message = "Ricordati di monitorare tutti i tuoi viaggi!",
            notificationId = System.currentTimeMillis().toInt(),
            iconRes = android.R.drawable.ic_dialog_info,
            channelDescription = "Notifica evento geofence generica"
        )
    }
}