package com.example.travelcompanion.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.android.libraries.places.api.model.Place

object NotificationUtils {

    fun sendNotification(
        context: Context,
        channelId: String,
        channelName: String,
        title: String,
        message: String,
        notificationId: Int,
        iconRes: Int,
        channelDescription: String = "Notifiche di Travel Companion", // TODO: Move to string resources
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        largeIcon: android.graphics.Bitmap? = null,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Create channel only if not already created
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existingChannel = notificationManager.getNotificationChannel(channelId)
            if (existingChannel == null) {
                val channel = NotificationChannel(
                    channelId,
                    channelName,
                    importance
                )
                channel.description = channelDescription
                notificationManager.createNotificationChannel(channel)
            }
        }

        // Create PendingIntent to open the app when notification is tapped
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launchIntent?.let {
            androidx.core.app.TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(it)
                getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title) // TODO: Move to string resources
            .setContentText(message) // TODO: Move to string resources
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Set large icon if provided
        largeIcon?.let { builder.setLargeIcon(it) }

        // Add actions if provided
        actions.forEach { builder.addAction(it) }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    fun sendPoiNotification(context: Context, poiName: String) {
        sendNotification(
            context = context,
            channelId = "poi_channel",
            channelName = "Punti di Interesse", // TODO: Move to string resources
            title = "Sei vicino a $poiName", // TODO: Move to string resources
            message = "Dai un’occhiata!", // TODO: Move to string resources
            notificationId = poiName.hashCode(),
            iconRes = android.R.drawable.ic_dialog_map,
            channelDescription = "Notifiche relative ai punti di interesse" // TODO: Move to string resources
        )
    }

    fun checkDistanceAndNotify(context: Context, distance: Double, place: Place) {
        if (distance < 10) {
            sendPoiNotification(context, place.name ?: "Un luogo interessante")
        }
    }
}