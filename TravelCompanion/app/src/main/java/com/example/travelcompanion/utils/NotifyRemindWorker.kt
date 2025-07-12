package com.example.travelcompanion.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import kotlinx.coroutines.runBlocking

class NotifyRemindWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result = runBlocking {
        val repo = com.example.travelcompanion.repository.TravelRepository.create(applicationContext)
        val millis = repo.getLastTripTimestamp()

        if (millis != null) {
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            if (millis < thirtyDaysAgo) {
                sendNotification("È più di un mese che non viaggi!")
            }
        } else {
            val activeTrip = repo.getActiveTripId()
            if (activeTrip == null) {
                sendNotification("Avvia il tuo primo viaggio")
            }
        }

        Result.success()
    }


    private fun sendNotification(message: String) {
        val notificationManager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "trip_reminder_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Promemoria Viaggi",
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setContentTitle("Travel Companion")
            .setContentText(message)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        notificationManager.notify(1, notification)
    }
}