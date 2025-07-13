package com.example.travelcompanion

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
        
        // Load and display user profile picture in drawer header
        val headerView = navView.getHeaderView(0)
        val userPhoto = headerView.findViewById<ImageView>(R.id.user_photo)
        CoroutineScope(Dispatchers.IO).launch {
            val db = TravelDatabase.getDatabase(applicationContext)
            val user = db.userDao().getUserById(1L) // Change as needed for multi-user
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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment_content_main)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }
}