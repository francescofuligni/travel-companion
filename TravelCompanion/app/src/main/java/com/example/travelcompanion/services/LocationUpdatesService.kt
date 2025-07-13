package com.example.travelcompanion.services

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.IBinder
import android.os.Looper
import androidx.core.app.ActivityCompat
import com.example.travelcompanion.R
import com.google.android.gms.location.*
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FindCurrentPlaceRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.example.travelcompanion.services.BaseLocationService
import com.example.travelcompanion.utils.NotificationUtils
import com.google.android.libraries.places.api.model.PlaceLikelihood
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * Servizio per il monitoraggio della posizione e notifiche POI
 * Invia notifiche quando l'utente si trova vicino a luoghi interessanti
 */
class LocationUpdatesService : BaseLocationService() {

    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback
    private lateinit var placesClient: PlacesClient
    
    // Cache per evitare notifiche duplicate
    private val notifiedPlaces = mutableSetOf<String>()
    private var lastPlaceCheckTime = 0L
    private val PLACE_CHECK_INTERVAL = 3 * 60 * 1000L // 3 minuti
    private val NOTIFICATION_COOLDOWN = 20 * 60 * 1000L // 20 minuti per stessi posti

    override fun onCreate() {
        super.onCreate()

        startForegroundServiceWithNotification()

        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        initFusedLocationClient()

        locationRequest = buildHighAccuracyRequest(
            intervalMillis = 2 * 60 * 1000L,
            minUpdateMillis = 60 * 1000L
        )

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { location ->
                    android.util.Log.d("LocationService", "Nuova posizione: ${location.latitude}, ${location.longitude}")
                    val currentTime = System.currentTimeMillis()
                    if (currentTime - lastPlaceCheckTime > PLACE_CHECK_INTERVAL) {
                        checkNearbyPlaces(location)
                        lastPlaceCheckTime = currentTime
                    }
                }
            }
        }

        startLocationUpdates()
    }

    /**
     * Avvia il servizio in foreground con notifica persistente
     */
    private fun startForegroundServiceWithNotification() {
        val channelId = "location_foreground"
        val channelName = "Monitoraggio posizione"
        val notificationId = 10001

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                channelId,
                channelName,
                android.app.NotificationManager.IMPORTANCE_LOW
            )
            channel.description = "Notifica persistente per il monitoraggio della posizione e POI"
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(this, channelId)
            .setContentTitle("Travel Companion attivo")
            .setContentText("Monitoraggio posizione per notifiche POI e geofence attivo")
            .setSmallIcon(android.R.drawable.star_on)
            .setOngoing(true)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(notificationId, notification)
    }

    /**
     * Avvia gli aggiornamenti di localizzazione
     */
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.e("LocationService", "Permessi di localizzazione non concessi")
            return
        }
        
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )
        android.util.Log.d("LocationService", "Aggiornamenti di localizzazione avviati")
    }

    /**
     * Controlla i luoghi interessanti nelle vicinanze
     */
    private fun checkNearbyPlaces(location: Location) {
        android.util.Log.d("LocationService", "=== CONTROLLO LUOGHI NELLE VICINANZE ===")
        android.util.Log.d("LocationService", "Posizione corrente: ${location.latitude}, ${location.longitude}")
        
        val placeFields = listOf(
            Place.Field.NAME, 
            Place.Field.LAT_LNG,
            Place.Field.TYPES,
            Place.Field.ID,
            Place.Field.RATING
        )

        val request = FindCurrentPlaceRequest.newInstance(placeFields)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            android.util.Log.e("LocationService", "Permessi non concessi per findCurrentPlace")
            return
        }
        
        placesClient.findCurrentPlace(request)
            .addOnSuccessListener { response ->
                android.util.Log.d("LocationService", "Luoghi trovati: ${response.placeLikelihoods.size}")
                processFindPlaceResults(response.placeLikelihoods, location)
            }
            .addOnFailureListener { exception ->
                android.util.Log.e("LocationService", "Errore findCurrentPlace: ${exception.message}")
                android.util.Log.e("LocationService", "Exception details: ", exception)
            }
    }

    /**
     * Processa i risultati dei luoghi trovati
     */
    private fun processFindPlaceResults(
        placeLikelihoods: List<PlaceLikelihood>,
        userLocation: Location
    ) {
        for (placeLikelihood in placeLikelihoods) {
            val place = placeLikelihood.place
            val poiLocation = place.latLng ?: continue
            val placeId = place.id ?: continue
            
            val distance = calculateDistance(
                userLocation.latitude, userLocation.longitude,
                poiLocation.latitude, poiLocation.longitude
            )
            
            android.util.Log.d("LocationService", "Posto: ${place.name}, distanza: $distance metri")
            
            if (distance < 100 &&
                isInterestingPlace(place) && 
                shouldNotifyForPlace(placeId)
            ) {
                val placeName = place.name ?: "Un luogo interessante"
                android.util.Log.d("LocationService", "Invio notifica per: $placeName")
                
                NotificationUtils.sendPoiNotification(applicationContext, placeName)
                
                notifiedPlaces.add(placeId)
                
                CoroutineScope(Dispatchers.IO).launch {
                    delay(NOTIFICATION_COOLDOWN)
                    notifiedPlaces.remove(placeId)
                }
                
                break
            }
        }
    }

    /**
     * Determina se un luogo è interessante per il viaggiatore
     */
    private fun isInterestingPlace(place: Place): Boolean {
        val interestingTypes = setOf(
            Place.Type.TOURIST_ATTRACTION,
            Place.Type.MUSEUM,
            Place.Type.RESTAURANT,
            Place.Type.NATURAL_FEATURE,
            Place.Type.PARK,
            Place.Type.POINT_OF_INTEREST,
            Place.Type.ESTABLISHMENT,
            Place.Type.CHURCH,
            Place.Type.STORE,
            Place.Type.SHOPPING_MALL
        )
        
        val hasInterestingType = place.types?.any { type -> 
            interestingTypes.contains(type) 
        } ?: false
        
        android.util.Log.d("LocationService", "Posto: ${place.name}, tipi: ${place.types}, interessante: $hasInterestingType")
        return hasInterestingType
    }

    /**
     * Controlla se dobbiamo notificare per questo luogo
     */
    private fun shouldNotifyForPlace(placeId: String): Boolean {
        return !notifiedPlaces.contains(placeId)
    }

    override fun onDestroy() {
        super.onDestroy()
        fusedLocationClient.removeLocationUpdates(locationCallback)
        android.util.Log.d("LocationService", "Servizio LocationUpdatesService terminato")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}