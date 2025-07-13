package com.example.travelcompanion.services

import android.Manifest
import android.app.*
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.example.travelcompanion.database.TravelDatabase
import com.example.travelcompanion.database.models.Location
import com.example.travelcompanion.database.models.TripPhase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.android.gms.location.*

class TrackingService : BaseLocationService() {

    private var phaseOrderCounter = 0
    private var endDate: Long = -1L

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var tripId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        initFusedLocationClient()

        locationRequest = buildHighAccuracyRequest(
            intervalMillis = 30_000L,
            minUpdateMillis = 30_000L
        )

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    handleNewLocation(location)
                }
            }
        }

        startForegroundNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val tripIdString = intent?.getStringExtra("TRIP_ID")
        tripId = tripIdString?.toLongOrNull() ?: -1L
        endDate = intent?.getLongExtra("END_DATE", -1L) ?: -1L
        
        if (tripId != -1L) {
            // Initialize phase order counter
            phaseOrderCounter = 0
            startLocationUpdates()
            Log.d("TrackingService", "Tracking iniziato per trip ID: $tripId")
        } else {
            Log.e("TrackingService", "Trip ID non valido")
            stopSelf()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startForegroundNotification() {
        val channelId = "tracking_channel"
        val channelName = "Location Tracking"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, channelName, NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("TravelCompanion in viaggio")
            .setContentText("Monitoraggio posizione attivo")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .build()

        startForeground(1, notification)
    }

    private fun startLocationUpdates() {
        if (
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED
        ) {
            Log.e("TrackingService", "Permessi di localizzazione non concessi")
            stopSelf()
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            mainLooper
        )
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    private fun handleNewLocation(location: android.location.Location) {
        val db = TravelDatabase.getDatabase(applicationContext)
        if (System.currentTimeMillis() > endDate && endDate > 0) {
            Log.d("TrackingService", "Fine viaggio raggiunta. Interrompo il tracking.")
            // Aggiorna il viaggio come terminato (solo isActive, preserva endDate)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val trip = db.tripDao().getTripById(tripId)
                    if (trip != null && trip.isActive) {
                        val updatedTrip = trip.copy(
                            isActive = false
                        )
                        db.tripDao().updateTrip(updatedTrip)
                        Log.d("TrackingService", "Trip aggiornato come terminato automaticamente: ${updatedTrip.id}")
                    }
                } catch (e: Exception) {
                    Log.e("TrackingService", "Errore aggiornamento trip a fine automatica", e)
                }
                stopSelf()
            }
            return
        }
        Log.d("TrackingService", "Nuova posizione: ${location.latitude}, ${location.longitude}")

        val radius = 0.0003 // ~30m in lat/lon approssimato

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val existingLocation = db.locationDao().findWithinRadius(
                    location.latitude - radius, location.latitude + radius,
                    location.longitude - radius, location.longitude + radius
                )

                val locationId = if (existingLocation != null) {
                    Log.d("TrackingService", "Using existing location ID: ${existingLocation.id}")
                    existingLocation.id
                } else {
                    val newLocationId = db.locationDao().insertLocation(
                        Location(
                            id = 0,
                            latitude = location.latitude,
                            longitude = location.longitude
                        )
                    )
                    Log.d("TrackingService", "Created new location ID: $newLocationId")
                    newLocationId
                }

                val tripPhase = TripPhase(
                    id = 0,
                    tripId = tripId,
                    locationId = locationId,
                    timestamp = System.currentTimeMillis(),
                    phaseOrder = phaseOrderCounter
                )
                
                val insertedPhaseId = db.tripPhaseDao().insertPhase(tripPhase)
                Log.d("TrackingService", "Fase inserita con ID: $insertedPhaseId, order: $phaseOrderCounter, tripId: $tripId")
                
                phaseOrderCounter++
                updateDistanceAndDuration(db, tripPhase.copy(id = insertedPhaseId))
            } catch (e: Exception) {
                Log.e("TrackingService", "Errore durante l'inserimento fase", e)
            }
        }
    }

    private suspend fun updateDistanceAndDuration(db: TravelDatabase, tripPhase: TripPhase) {
        val location = db.locationDao().getLocationById(tripPhase.locationId)
        if (phaseOrderCounter > 1) {
            val previousPhase = db.tripPhaseDao().getLastPhaseBefore(tripId, phaseOrderCounter - 1)
            val previousLocation = previousPhase?.let { db.locationDao().getLocationById(it.locationId) }

            if (previousLocation != null) {
                val distance = if (location != null) {
                    calculateDistance(
                        previousLocation.latitude, previousLocation.longitude,
                        location.latitude, location.longitude
                    )
                } else 0.0

                val trip = db.tripDao().getTripById(tripId)
                if (trip != null) {
                    val updatedDistance = trip.distance + distance
                    val updatedDuration = trip.duration + 30.0 // 30 seconds per update
                    val updatedTrip = trip.copy(distance = updatedDistance, duration = updatedDuration)
                    db.tripDao().updateTrip(updatedTrip)
                    Log.d("TrackingService", "Trip updated: distance=${updatedDistance}m, duration=${updatedDuration}s")
                }
            }
        }
        Log.d("TrackingService", "Distanza incrementata e tempo di viaggio incrementato.")
    }  
}
