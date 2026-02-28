package com.example.ontrack.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.app.NotificationChannel
import android.app.NotificationManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ontrack.MainActivity

private const val CHANNEL_ID = "ontrack_timer_finished"
private const val NOTIFICATION_ID = 1001

/**
 * Shows a notification when a task timer has finished. All text in English.
 * Uses default notification sound so the user hears an alert.
 * Tapping the notification opens the app (MainActivity).
 */
fun showTimerFinishedNotification(context: Context, habitTitle: String) {
    val channel = NotificationChannel(
        CHANNEL_ID,
        "Timer",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setShowBadge(true)
        enableVibration(true)
        setSound(
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
            null
        )
    }
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(channel)

    val openAppIntent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
        context,
        NOTIFICATION_ID,
        openAppIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Time's up")
        .setContentText("Task completed: $habitTitle")
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .build()
    try {
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    } catch (_: Exception) { }
}

/**
 * Plays the default notification sound (e.g. when app is in foreground).
 */
fun playTimerFinishedSound(context: Context) {
    try {
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val ringtone = RingtoneManager.getRingtone(context, uri)
        ringtone?.play()
    } catch (_: Exception) { }
}
