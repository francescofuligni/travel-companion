package com.example.travelcompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver per BOOT_COMPLETED: ri-registra il geofence casa dopo il riavvio del device
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device riavviato: provo a ri-registrare il geofence casa")
            val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
            val homeLat = prefs.getFloat("home_latitude", Float.MIN_VALUE)
            val homeLon = prefs.getFloat("home_longitude", Float.MIN_VALUE)
            if (homeLat != Float.MIN_VALUE && homeLon != Float.MIN_VALUE) {
                HomeGeofenceService.registerHomeGeofence(
                    context = context,
                    latitude = homeLat.toDouble(),
                    longitude = homeLon.toDouble()
                )
            } else {
                Log.w("BootCompletedReceiver", "Posizione casa non impostata, nessun geofence registrato")
            }
        }
    }
}
