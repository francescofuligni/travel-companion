package com.example.travelcompanion.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver per BOOT_COMPLETED: ri-registra il geofence casa dopo il riavvio del device
 * Garantisce che il geofencing rimanga attivo anche dopo il riavvio del dispositivo
 */
class BootCompletedReceiver : BroadcastReceiver() {
    
    /**
     * Gestisce l'evento di riavvio del dispositivo
     */
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        val prefs = context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val homeLat = prefs.getFloat("home_latitude", Float.MIN_VALUE)
        val homeLon = prefs.getFloat("home_longitude", Float.MIN_VALUE)


        if (action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device riavviato: ri-registrazione geofence casa")
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
