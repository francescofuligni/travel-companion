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
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import com.example.travelcompanion.databinding.ActivityMainBinding
import android.widget.ImageView
import com.example.travelcompanion.database.TravelDatabase
import com.bumptech.glide.Glide
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.work.WorkManager
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.ExistingPeriodicWorkPolicy
import java.util.concurrent.TimeUnit
import com.example.travelcompanion.services.NotificationRemindWorker
import com.example.travelcompanion.services.HomeGeofenceService

class MainActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.appBarMain.toolbar)

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
        navView.setupWithNavController(navController)
        
        loadUserProfilePicture(navView.getHeaderView(0))
        scheduleTripReminderWorker()
        startLocationService()
        registerHomeGeofenceIfSet()
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
            1, TimeUnit.DAYS
        ).build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "trip_reminder_worker",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    private fun startLocationService() {
        val locationServiceIntent = Intent(this, com.example.travelcompanion.services.LocationUpdatesService::class.java)
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

    private fun loadUserProfilePicture(headerView: android.view.View) {
        val userPhoto = headerView.findViewById<ImageView>(R.id.user_photo)
        CoroutineScope(Dispatchers.IO).launch {
            val db = TravelDatabase.getDatabase(applicationContext)
            val user = db.userDao().getUserById(1L)
            val profilePicId = user?.profilePictureId
            if (profilePicId != null) {
                val image = db.imageDao().getImageById(profilePicId)
                val uri = image?.uri
                if (!uri.isNullOrBlank()) {
                    launch(Dispatchers.Main) {
                        Glide.with(this@MainActivity)
                            .load(uri)
                            .placeholder(R.drawable.missing_img)
                            .error(R.drawable.missing_img)
                            .circleCrop()
                            .into(userPhoto)
                    }
                }
            }
        }
    }
}