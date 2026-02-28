package com.example.ontrack.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.ontrack.receiver.SleepReminderReceiver
import java.util.Calendar

private const val REQUEST_SLEEP = 2001
private const val REQUEST_WAKE = 2002

/**
 * Schedules daily sleep and wake notifications based on device time.
 * bedtimeMinutes and wakeMinutes are minutes since midnight (0–1439).
 */
object SleepReminderScheduler {

    fun schedule(context: Context, bedtimeMinutes: Int, wakeMinutes: Int) {
        if (bedtimeMinutes < 0 || wakeMinutes < 0) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val calendar = Calendar.getInstance()

        // Bedtime alarm
        val sleepIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = SleepReminderReceiver.ACTION_SLEEP
        }
        val sleepPending = PendingIntent.getBroadcast(
            context,
            REQUEST_SLEEP,
            sleepIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        calendar.set(Calendar.HOUR_OF_DAY, bedtimeMinutes / 60)
        calendar.set(Calendar.MINUTE, bedtimeMinutes % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var triggerSleep = calendar.timeInMillis
        if (triggerSleep <= System.currentTimeMillis()) triggerSleep += 24 * 60 * 60 * 1000
        setExactAlarm(alarmManager, triggerSleep, sleepPending)

        // Wake alarm
        val wakeIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = SleepReminderReceiver.ACTION_WAKE
        }
        val wakePending = PendingIntent.getBroadcast(
            context,
            REQUEST_WAKE,
            wakeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        calendar.set(Calendar.HOUR_OF_DAY, wakeMinutes / 60)
        calendar.set(Calendar.MINUTE, wakeMinutes % 60)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        var triggerWake = calendar.timeInMillis
        if (triggerWake <= System.currentTimeMillis()) triggerWake += 24 * 60 * 60 * 1000
        setExactAlarm(alarmManager, triggerWake, wakePending)
    }

    fun cancel(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sleepIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = SleepReminderReceiver.ACTION_SLEEP
        }
        val wakeIntent = Intent(context, SleepReminderReceiver::class.java).apply {
            action = SleepReminderReceiver.ACTION_WAKE
        }
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                REQUEST_SLEEP,
                sleepIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
        alarmManager.cancel(
            PendingIntent.getBroadcast(
                context,
                REQUEST_WAKE,
                wakeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    private fun setExactAlarm(alarmManager: AlarmManager, triggerTime: Long, pending: PendingIntent) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
            }
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pending)
        }
    }
}
