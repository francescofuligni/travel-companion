package com.example.travelcompanion.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationUtils {

    /*
     * Sends a notification with customizable parameters
     * Creates notification channel if it doesn't exist (API 26+)
     */
    fun sendNotification(
        context: Context,
        channelId: String,
        channelName: String,
        title: String,
        message: String,
        notificationId: Int,
        iconRes: Int,
        channelDescription: String = "Notifiche di Travel Companion",
        importance: Int = NotificationManager.IMPORTANCE_DEFAULT,
        largeIcon: android.graphics.Bitmap? = null,
        actions: List<NotificationCompat.Action> = emptyList()
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

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

        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pendingIntent = launchIntent?.let {
            androidx.core.app.TaskStackBuilder.create(context).run {
                addNextIntentWithParentStack(it)
                getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            }
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        largeIcon?.let { builder.setLargeIcon(it) }
        actions.forEach { builder.addAction(it) }

        val notification = builder.build()
        notificationManager.notify(notificationId, notification)
    }

    /*
     * Sends a notification when user is near a point of interest
     */
    fun sendPoiNotification(context: Context, poiName: String) {
        sendNotification(
            context = context,
            channelId = "poi_channel",
            channelName = "Punti di Interesse",
            title = "Sei vicino a $poiName",
            message = "Dai un'occhiata!",
            notificationId = poiName.hashCode(),
            iconRes = android.R.drawable.ic_dialog_map,
            channelDescription = "Notifiche relative ai punti di interesse"
        )
    }
}