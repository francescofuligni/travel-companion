package com.example.travelcompanion

import android.content.Intent
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

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.appBarMain.toolbar)
        // devo chiedere il pemesso
        if (android.os.Build.VERSION.SDK_INT >= 33) {
            if (checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1001)
            }
        }

        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navView: NavigationView = binding.navView
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
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
        // Collega NavigationView al NavController
        navView.setupWithNavController(navController)

        // Assicura che il header del drawer sia gonfiato
        if (navView.headerCount == 0) {
            navView.inflateHeaderView(R.layout.nav_header_main)
        }

        // Gestisci selezione e reselezione dei menu
        navView.setNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    // Torna a Home e ricarica se già in Home
                    val navOptions = NavOptions.Builder()
                        .setPopUpTo(navController.graph.id, false)
                        .build()
                    navController.navigate(R.id.nav_home, null, navOptions)
                    val hostFragment = supportFragmentManager.findFragmentById(R.id.nav_host_fragment_content_main)
                    if (hostFragment is NavHostFragment) {
                        val current = hostFragment.childFragmentManager.fragments.firstOrNull()
                        if (current is HomeFragment) current.forceReload()
                    }
                    binding.drawerLayout.closeDrawers()
                    true
                }
                else -> {
                    val handled = NavigationUI.onNavDestinationSelected(item, navController)
                    if (handled) binding.drawerLayout.closeDrawers()
                    handled
                }
            }
        }
        
        loadUserProfilePicture()
        scheduleTripReminderWorker()
        checkAndStartLocationServices()
    }

    private fun checkAndStartLocationServices() {
        val requiredPermissions = mutableListOf(
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.FOREGROUND_SERVICE
        )
        if (android.os.Build.VERSION.SDK_INT >= 29) {
            requiredPermissions.add(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        val notGranted = requiredPermissions.filter {
            checkSelfPermission(it) != android.content.pm.PackageManager.PERMISSION_GRANTED
        }
        if (notGranted.isEmpty()) {
            startLocationService()
            registerHomeGeofenceIfSet()
        } else {
            requestPermissions(notGranted.toTypedArray(), 1010)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    private fun scheduleTripReminderWorker() {
        val request = PeriodicWorkRequestBuilder<NotificationRemindWorker>(
            15, TimeUnit.MINUTES
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "trip_reminder_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun startLocationService() {
        val locationServiceIntent = Intent(this, LocationUpdatesService::class.java)
        startService(locationServiceIntent)
    }

    private fun registerHomeGeofenceIfSet() {
        val prefs = getSharedPreferences("user_prefs", MODE_PRIVATE)
        val homeLat = prefs.getFloat("home_latitude", Float.MIN_VALUE)
        val homeLon = prefs.getFloat("home_longitude", Float.MIN_VALUE)

        if (homeLat != Float.MIN_VALUE && homeLon != Float.MIN_VALUE) {
            HomeGeofenceService.registerHomeGeofence(
                context = this,
                latitude = homeLat.toDouble(),
                longitude = homeLon.toDouble()
            )
        }
    }

    private fun loadUserProfilePicture() {
        val headerView = binding.navView.getHeaderView(0)
        val ivProfile = headerView.findViewById<ImageView>(R.id.user_photo)
        lifecycleScope.launch {
            // Carica utente e immagine in background
            val user = withContext(Dispatchers.IO) {
                TravelDatabase.getDatabase(applicationContext).userDao().getUserById(1L)
            }
            val uri = user?.profilePictureId?.let { imageId ->
                withContext(Dispatchers.IO) {
                    TravelDatabase.getDatabase(applicationContext)
                        .imageDao()
                        .getImageById(imageId)
                        ?.uri
                }
            }
            // Aggiorna UI
            if (!uri.isNullOrBlank()) {
                Glide.with(this@MainActivity)
                    .load(uri)
                    .placeholder(R.drawable.missing_img)
                    .error(R.drawable.missing_img)
                    .circleCrop()
                    .into(ivProfile)
            } else {
                ivProfile.setImageResource(R.drawable.missing_img)
            }
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1010) {
            if (grantResults.isNotEmpty() && grantResults.all { it == android.content.pm.PackageManager.PERMISSION_GRANTED }) {
                startLocationService()
                registerHomeGeofenceIfSet()
            } else {
                // Optional: mostra un Toast per spiegare che senza permessi alcune funzionalità non sono disponibili
            }
        }
        // Gestisci anche POST_NOTIFICATIONS (requestCode == 1001) se necessario, come già fai.
    }
}