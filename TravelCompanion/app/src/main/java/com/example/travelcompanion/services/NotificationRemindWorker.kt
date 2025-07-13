package com.example.travelcompanion.services

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.travelcompanion.repository.TravelRepository
import com.example.travelcompanion.utils.NotificationUtils
import kotlinx.coroutines.runBlocking

class NotificationRemindWorker(context: Context, workerParams: WorkerParameters) : Worker(context, workerParams) {

    override fun doWork(): Result = runBlocking {
        val repo = TravelRepository.create(applicationContext)
        val millis = repo.getLastTripTimestamp()

        if (millis != null) {
            val thirtyDaysAgo = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
            if (millis < thirtyDaysAgo) {
                NotificationUtils.sendNotification(
                    context = applicationContext,
                    channelId = "trip_reminder_channel",
                    channelName = "Promemoria Viaggi",
                    title = "Travel Companion",
                    message = "È più di un mese che non viaggi!",
                    notificationId = 1,
                    iconRes = android.R.drawable.ic_dialog_info
                )
            }
        } else {
            val activeTrip = repo.getActiveTripId()
            if (activeTrip == null) {
                NotificationUtils.sendNotification(
                    context = applicationContext,
                    channelId = "trip_reminder_channel",
                    channelName = "Promemoria Viaggi",
                    title = "Travel Companion",
                    message = "Avvia il tuo primo viaggio",
                    notificationId = 1,
                    iconRes = android.R.drawable.ic_dialog_info
                )
            }
        }
        Result.success()
    }
}