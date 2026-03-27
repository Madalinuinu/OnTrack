package com.example.ontrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ontrack.OnTrackApplication
import com.example.ontrack.util.EffectiveDate
import com.example.ontrack.util.showTimerFinishedNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val EXTRA_HABIT_ID = "habit_id"
private const val EXTRA_SYSTEM_ID = "system_id"
private const val EXTRA_HABIT_TITLE = "habit_title"
private const val EXTRA_TOTAL_SECONDS = "total_seconds"

/**
 * When the app was killed and the timer expired, AlarmManager fires this receiver.
 * We complete the habit with duration and show the "Time's up" notification.
 */
class TimerExpiredReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        val systemId = intent.getLongExtra(EXTRA_SYSTEM_ID, 0L)
        val habitTitle = intent.getStringExtra(EXTRA_HABIT_TITLE) ?: ""
        val totalSeconds = intent.getIntExtra(EXTRA_TOTAL_SECONDS, 0)
        if (habitId == 0L || totalSeconds == 0) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            withContext(Dispatchers.IO) {
                val app = context.applicationContext as? OnTrackApplication ?: return@withContext
                val todayEpoch = EffectiveDate.todayEpoch()
                val durationMinutes = totalSeconds / 60
                app.database.habitLogDao().completeWithDuration(
                    habitId,
                    todayEpoch,
                    durationMinutes,
                    durationSeconds = totalSeconds
                )
                app.streakManager.refreshStreak(systemId)
                val systemIds = app.database.systemDao().getAllSystems().first().filter { !it.isTestData }.map { it.id }
                app.streakManager.refreshGlobalStreak(systemIds)
            }
            showTimerFinishedNotification(context, habitTitle)
            pendingResult.finish()
            scope.cancel()
        }
    }
}
