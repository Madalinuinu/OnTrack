package com.example.ontrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ontrack.OnTrackApplication
import com.example.ontrack.util.SleepReminderScheduler
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? OnTrackApplication ?: return
        runBlocking {
            val bedtime = app.userPreferences.sleepBedtimeMinutes.first()
            val wake = app.userPreferences.sleepWakeMinutes.first()
            if (bedtime >= 0 && wake >= 0) {
                SleepReminderScheduler.schedule(context, bedtime, wake)
            }
        }
    }
}
