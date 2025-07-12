package com.example.travelcompanion.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat

object NotificationUtils {

    fun sendNotification(
        context: Context,
        channelId: String,
        channelName: String,
        title: String,
        message: String,
        notificationId: Int,
        iconRes: Int
    ) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setContentTitle(title)
            .setContentText(message)
            .setSmallIcon(iconRes)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }

    fun sendPoiNotification(context: Context, poiName: String) {
        sendNotification(
            context = context,
            channelId = "poi_channel",
            channelName = "Punti di Interesse",
            title = "Sei vicino a $poiName",
            message = "Dai un’occhiata!",
            notificationId = poiName.hashCode(),
            iconRes = android.R.drawable.ic_dialog_map
        )
    }
}