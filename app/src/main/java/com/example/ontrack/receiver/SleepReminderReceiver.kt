package com.example.ontrack.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ontrack.MainActivity
import com.example.ontrack.R
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import com.example.ontrack.OnTrackApplication
import com.example.ontrack.util.SleepReminderScheduler

const val CHANNEL_SLEEP_REMINDER = "sleep_reminder"
private const val NOTIFICATION_SLEEP = 3001
private const val NOTIFICATION_WAKE = 3002

class SleepReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_SLEEP -> showSleepNotification(context)
            ACTION_WAKE -> showWakeNotification(context)
        }
        rescheduleNextDay(context)
    }

    private fun showSleepNotification(context: Context) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = android.app.PendingIntent.getActivity(
            context,
            NOTIFICATION_SLEEP,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP_REMINDER)
            .setSmallIcon(R.drawable.ic_notification_sleep)
            .setContentTitle("Time to sleep !!!")
            .setContentText("Get some rest.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_SLEEP, notification)
        } catch (_: Exception) { }
    }

    private fun showWakeNotification(context: Context) {
        ensureChannel(context)
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pending = android.app.PendingIntent.getActivity(
            context,
            NOTIFICATION_WAKE,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_SLEEP_REMINDER)
            .setSmallIcon(R.drawable.ic_notification_sun)
            .setContentTitle("Wake up and shine !!!")
            .setContentText("Start your day.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pending)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .build()
        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_WAKE, notification)
        } catch (_: Exception) { }
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_SLEEP_REMINDER,
                "Sleep reminder",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                setShowBadge(true)
                enableVibration(true)
                setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
            }
            (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun rescheduleNextDay(context: Context) {
        val app = context.applicationContext as? OnTrackApplication ?: return
        runBlocking {
            val bedtime = app.userPreferences.sleepBedtimeMinutes.first()
            val wake = app.userPreferences.sleepWakeMinutes.first()
            if (bedtime >= 0 && wake >= 0) {
                SleepReminderScheduler.schedule(context, bedtime, wake)
            }
        }
    }

    companion object {
        const val ACTION_SLEEP = "com.example.ontrack.SLEEP_REMINDER_SLEEP"
        const val ACTION_WAKE = "com.example.ontrack.SLEEP_REMINDER_WAKE"
    }
}
