package com.example.travelcompanion

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.Menu
import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.navigation.NavOptions
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.travelcompanion.databinding.ActivityMainBinding
import android.widget.ImageView
import androidx.core.app.ActivityCompat
import com.example.travelcompanion.database.TravelDatabase
import com.bumptech.glide.Glide
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.launch
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.travelcompanion.services.NotificationRemindWorker
import com.example.travelcompanion.services.HomeGeofenceService
import com.example.travelcompanion.services.LocationUpdatesService
import com.example.travelcompanion.ui.home.HomeFragment
import com.example.travelcompanion.utils.NotificationUtils
import android.Manifest
import android.os.Build
import androidx.appcompat.app.AlertDialog
import android.util.Log

/**
 * Activity principale dell'app Travel Companion
 * Gestisce la navigazione, i permessi, i servizi in background e l'interfaccia utente
 */
class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding
    
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 1001
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1002
        private const val BACKGROUND_LOCATION_REQUEST_CODE = 1003
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        initializeActivity()
        setupNavigation()
        initializeServices()
    }

    /**
     * Inizializza l'activity con binding e toolbar
     */
    private fun initializeActivity() {
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        
        // Richiedi permessi necessari
        requestRequiredPermissions()
    }

    /**
     * Configura la navigazione con drawer e controller
     */
    private fun setupNavigation() {
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        
        // Configurazione AppBar con destinazioni top-level
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home,
                R.id.nav_new_trip,
                R.id.nav_stats,
                R.id.nav_my_trips, 
                R.id.nav_settings
            ), drawerLayout
        )
        
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)

        // Assicura che il header del drawer sia gonfiato
        if (navView.headerCount == 0) {
            navView.inflateHeaderView(R.layout.nav_header_main)
        }

        setupNavigationItemListener(navView, navController)
    }

    /**
     * Configura il listener per la selezione degli item del drawer
     */
    private fun setupNavigationItemListener(navView: NavigationView, navController: androidx.navigation.NavController) {
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    handleHomeNavigation(navController)
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) binding.drawerLayout.closeDrawers()
                    handled
                }
            }
        }
    }

    /**
     * Gestisce la navigazione verso la home con reload forzato
     */
    private fun handleHomeNavigation(navController: androidx.navigation.NavController) {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(navController.graph.id, false)
            .build()
        navController.navigate(R.id.nav_home, null, navOptions)
        
        // Forza il reload del fragment home
        val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
        if (hostFragment is NavHostFragment) {
            val current = hostFragment.childFragmentManager.fragments.firstOrNull()
            if (current is HomeFragment) current.forceReload()
        }
        
        binding.drawerLayout.closeDrawers()
    }

    /**
     * Inizializza i servizi dell'app
     */
    private fun initializeServices() {
        loadUserProfilePicture()
        scheduleTripReminderWorker()
        
        // Inizializza servizi solo se i permessi sono stati concessi
        if (hasLocationPermissions()) {
            startLocationService()
            registerHomeGeofenceIfSet()
        }
    }

    /**
     * Richiede tutti i permessi necessari per l'app
     */
    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        // Permesso notifiche per Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Permessi localizzazione base
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }

        // Permesso foreground service location per Android 14+
        if (Build.VERSION.SDK_INT >= 34) {
            if (checkSelfPermission(Manifest.permission.FOREGROUND_SERVICE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            Log.d(TAG, "Richiesta permessi: ${permissionsToRequest.joinToString()}")
            requestPermissions(permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            // Se i permessi base ci sono, verifica il background location
            requestBackgroundLocationPermissionIfNeeded()
        }
    }

    /**
     * Richiede il permesso per la localizzazione in background (necessario per geofencing)
     */
    private fun requestBackgroundLocationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                showBackgroundLocationPermissionDialog()
            } else {
                Log.d(TAG, "Permesso background location già concesso")
            }
        }
    }

    /**
     * Mostra dialog esplicativo per il permesso background location
     */
    private fun showBackgroundLocationPermissionDialog() {
        AlertDialog.Builder(this)
            .setTitle("Permesso Localizzazione in Background")
            .setMessage("Per ricevere notifiche quando esci da casa e rilevare punti di interesse nelle vicinanze, l'app ha bisogno del permesso di accesso alla posizione in background.")
            .setPositiveButton("Concedi") { _, _ ->
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    requestPermissions(
                        arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION),
                        BACKGROUND_LOCATION_REQUEST_CODE
                    )
                }
            }
            .setNegativeButton("Salta") { _, _ ->
                Log.w(TAG, "Permesso background location rifiutato dall'utente")
            }
            .show()
    }

    /**
     * Verifica se tutti i permessi di localizzazione necessari sono stati concessi
     */
    private fun hasLocationPermissions(): Boolean {
        val fineLocation = checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarseLocation = checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val backgroundLocation = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Non richiesto per Android < 10
        }
        return fineLocation && coarseLocation && backgroundLocation
    }

    /**
     * Verifica se il permesso background location è stato concesso
     */
    private fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkSelfPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Non necessario per Android < 10
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        
        when (requestCode) {
            PERMISSION_REQUEST_CODE -> {
                handleBasicPermissionsResult(grantResults)
            }
            BACKGROUND_LOCATION_REQUEST_CODE -> {
                handleBackgroundLocationPermissionResult(grantResults)
            }
        }
    }

    /**
     * Gestisce il risultato della richiesta dei permessi base
     */
    private fun handleBasicPermissionsResult(grantResults: IntArray) {
        Log.d(TAG, "Risultati permessi base: ${grantResults.joinToString()}")
        if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            Log.d(TAG, "Permessi base concessi")
            // Avvia servizi se i permessi sono stati concessi
            startLocationService()
            registerHomeGeofenceIfSet()
            // Richiedi background location se necessario
            requestBackgroundLocationPermissionIfNeeded()
        } else {
            Log.w(TAG, "Alcuni permessi base sono stati rifiutati")
        }
    }

    /**
     * Gestisce il risultato della richiesta del permesso background location
     */
    private fun handleBackgroundLocationPermissionResult(grantResults: IntArray) {
        if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "Permesso background location concesso")
            // Riavvia i servizi per abilitare il geofencing
            registerHomeGeofenceIfSet()
        } else {
            Log.w(TAG, "Permesso background location rifiutato")
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    /**
     * Programma il worker per i reminder periodici dei viaggi
     */
    private fun scheduleTripReminderWorker() {
        val request = PeriodicWorkRequestBuilder<NotificationRemindWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "trip_reminder_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
        Log.d(TAG, "Worker per reminder programmato")
    }

    /**
     * Avvia il servizio di localizzazione per rilevamento POI
     */
    private fun startLocationService() {
        if (hasLocationPermissions()) {
            val locationServiceIntent = Intent(this, LocationUpdatesService::class.java)
            startService(locationServiceIntent)
            Log.d(TAG, "Servizio localizzazione avviato")
        } else {
            Log.w(TAG, "Impossibile avviare servizio localizzazione: permessi mancanti")
        }
    }

    /**
     * Registra il geofence della casa se la posizione è stata impostata
     */
    private fun registerHomeGeofenceIfSet() {
        Log.d(TAG, "=== CONTROLLO GEOFENCE CASA ===")
        
        // Verifica permessi necessari (foreground + background)
        if (!hasLocationPermissions()) {
            Log.e(TAG, "Permessi localizzazione (foreground o background) mancanti per geofencing")
            showGeofencePermissionError()
            return
        }

        val homeLocation = getHomeLocationFromPreferences()
        if (homeLocation != null) {
            Log.d(TAG, "Registrazione geofence casa")
            HomeGeofenceService.registerHomeGeofence(
                context = this,
                latitude = homeLocation.first,
                longitude = homeLocation.second
            )
        } else {
            Log.w(TAG, "Posizione casa non impostata")
        }
    }

    /**
     * Recupera la posizione casa dalle SharedPreferences
     */
    private fun getHomeLocationFromPreferences(): Pair<Double, Double>? {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val homeLat = prefs.getFloat("home_latitude", Float.MIN_VALUE)
        val homeLon = prefs.getFloat("home_longitude", Float.MIN_VALUE)

        Log.d(TAG, "Posizione casa: lat=$homeLat, lon=$homeLon")

        return if (homeLat != Float.MIN_VALUE && homeLon != Float.MIN_VALUE) {
            Pair(homeLat.toDouble(), homeLon.toDouble())
        } else {
            null
        }
    }

    /**
     * Mostra notifica di errore per i permessi del geofencing
     */
    private fun showGeofencePermissionError() {
        NotificationUtils.sendNotification(
            context = this,
            channelId = "geofence_channel",
            channelName = "Geofence Error",
            title = "Permessi mancanti",
            message = "Per il geofencing sono necessari i permessi di localizzazione in primo piano e in background.",
            notificationId = 994,
            iconRes = android.R.drawable.ic_dialog_alert,
            channelDescription = "Errore permessi geofence"
        )
    }

    /**
     * Carica l'immagine del profilo utente nel drawer di navigazione
     */
    private fun loadUserProfilePicture() {
        val headerView = binding.navView.getHeaderView(0)
        val ivProfile = headerView.findViewById<ImageView>(R.id.user_photo)
        
        lifecycleScope.launch {
            try {
                val profileUri = getUserProfilePictureUri()
                updateProfilePictureInDrawer(ivProfile, profileUri)
            } catch (e: Exception) {
                Log.e(TAG, "Errore caricamento immagine profilo", e)
                ivProfile.setImageResource(R.drawable.missing_img)
            }
        }
    }

    /**
     * Recupera l'URI dell'immagine del profilo dal database
     */
    private suspend fun getUserProfilePictureUri(): String? {
        return withContext(Dispatchers.IO) {
            val user = TravelDatabase.getDatabase(applicationContext).userDao().getUserById(1L)
            user?.profilePictureId?.let { imageId ->
                TravelDatabase.getDatabase(applicationContext)
                    .imageDao()
                    .getImageById(imageId)
                    ?.uri
            }
        }
    }

    /**
     * Aggiorna l'immagine del profilo nel drawer usando Glide
     */
    private fun updateProfilePictureInDrawer(imageView: ImageView, uri: String?) {
        if (!uri.isNullOrBlank()) {
            Glide.with(this@MainActivity)
                .load(uri)
                .placeholder(R.drawable.missing_img)
                .error(R.drawable.missing_img)
                .circleCrop()
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.missing_img)
        }
    }
}