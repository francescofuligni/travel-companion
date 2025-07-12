package com.example.travelcompanion.utils

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

class TrackingService : Service() {

    private var phaseOrderCounter = 0
    private var endDate: Long = -1L

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private var tripId: Long = -1L

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 30000L)
            .setMinUpdateIntervalMillis(30000L)
            .build()

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
            startLocationUpdates()
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
        if (System.currentTimeMillis() > endDate && endDate > 0) {
            Log.d("TrackingService", "Fine viaggio raggiunta. Interrompo il tracking.")
            stopSelf()
            return
        }
        Log.d("TrackingService", "Nuova posizione: ${location.latitude}, ${location.longitude}")

        val db = TravelDatabase.getDatabase(applicationContext)
        val radius = 0.0002 // ~20m in lat/lon approssimato

        CoroutineScope(Dispatchers.IO).launch {
            val existingLocation = db.locationDao().findWithinRadius(
                location.latitude - radius, location.latitude + radius,
                location.longitude - radius, location.longitude + radius
            )

            val locationId = if (existingLocation != null) {
                existingLocation.id
            } else {
                db.locationDao().insertLocation(
                    Location(
                        id = 0,
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                )
            }

            val tripPhase = TripPhase(
                id = 0,
                tripId = tripId,
                locationId = locationId,
                timestamp = System.currentTimeMillis(),
                phaseOrder = phaseOrderCounter
            )
            phaseOrderCounter++
            db.tripPhaseDao().insertPhase(tripPhase)

            updateDistanceAndDuration(db, tripPhase)
        }
    }

    private suspend fun updateDistanceAndDuration(db: TravelDatabase, tripPhase: TripPhase) {
        val location = db.locationDao().getLocationById(tripPhase.locationId)
        if (phaseOrderCounter > 1) {
            val previousPhase = db.tripPhaseDao().getLastPhaseBefore(tripId, phaseOrderCounter - 1)
            val previousLocation = previousPhase?.let { db.locationDao().getLocationById(it.locationId) }

            if (previousLocation != null) {
                val results = FloatArray(1)
                if (location != null) {
                    android.location.Location.distanceBetween(
                        previousLocation.latitude, previousLocation.longitude,
                        location.latitude, location.longitude,
                        results
                    )
                }
                val distance = results[0].toDouble() // in metri

                val trip = db.tripDao().getTripById(tripId)
                if (trip != null) {
                    val updatedDistance = trip.distance + distance
                    val updatedDuration = trip.duration + 0.5
                    db.tripDao().updateTrip(
                        trip.copy(distance = updatedDistance, duration = updatedDuration)
                    )
                }
            }
        }
        Log.d("TrackingService", "Distanza incrementata e tempo di viaggio incrementato.")
    }  
}
