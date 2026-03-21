package com.example.ontrack.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.ontrack.OnTrackApplication
import com.example.ontrack.util.EffectiveDate
import com.example.ontrack.util.cancelOngoingTaskNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Only [ACTION_MARK_FINISHED] is handled here. Opening the app uses [PendingIntent.getActivity] on
 * [com.example.ontrack.MainActivity] so it never marks the task done.
 */
class OngoingTaskReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_MARK_FINISHED) return

        val habitId = intent.getLongExtra(EXTRA_HABIT_ID, 0L)
        val systemId = intent.getLongExtra(EXTRA_SYSTEM_ID, 0L)
        val dateEpoch = intent.getLongExtra(EXTRA_DATE_EPOCH, EffectiveDate.todayEpoch())
        if (habitId == 0L || systemId == 0L) return

        val pendingResult = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            withContext(Dispatchers.IO) {
                val app = context.applicationContext as? OnTrackApplication ?: return@withContext
                val log = app.database.habitLogDao().getLog(habitId, dateEpoch)
                if (log?.isCompleted != true && log?.isOngoing == true) {
                    if (app.userPreferences.streakSuppressEpochDay.first() == dateEpoch) {
                        app.userPreferences.clearStreakSuppress()
                    }
                    app.database.habitLogDao().completeOngoingAsDone(habitId, dateEpoch)
                    app.streakManager.refreshStreak(systemId)
                    val systemIds = app.database.systemDao().getAllSystems().first().filter { !it.isTestData }.map { it.id }
                    app.streakManager.refreshGlobalStreak(systemIds)
                }
                cancelOngoingTaskNotification(context, habitId)
            }
            pendingResult.finish()
            scope.cancel()
        }
    }

    companion object {
        const val ACTION_MARK_FINISHED = "com.example.ontrack.ONGOING_MARK_FINISHED"
        const val EXTRA_HABIT_ID = "ongoing_habit_id"
        const val EXTRA_SYSTEM_ID = "ongoing_system_id"
        const val EXTRA_DATE_EPOCH = "ongoing_date_epoch"
    }
}
