package com.example.ontrack.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.ServiceInfo
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.example.ontrack.OnTrackApplication
import com.example.ontrack.receiver.TimerExpiredReceiver
import com.example.ontrack.util.EffectiveDate
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val CHANNEL_ID = "ontrack_timer_running"
private const val NOTIFICATION_ID = 2000
private const val ACTION_PAUSE = "com.example.ontrack.timer.PAUSE"
private const val ACTION_RESUME = "com.example.ontrack.timer.RESUME"
private const val ACTION_CANCEL = "com.example.ontrack.timer.CANCEL"
private const val EXTRA_HABIT_ID = "habit_id"
private const val EXTRA_SYSTEM_ID = "system_id"
private const val EXTRA_HABIT_TITLE = "habit_title"
private const val EXTRA_TOTAL_SECONDS = "total_seconds"
private const val EXTRA_END_TIME_MILLIS = "end_time_millis"

class TimerForegroundService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val handler = Handler(Looper.getMainLooper())
    private var remainingSeconds = 0
    private var habitId = 0L
    private var systemId = 0L
    private var habitTitle = ""
    private var totalSeconds = 0
    private var isPaused = false
    private var endTimeMillis = 0L

    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isPaused) {
                handler.postDelayed(this, 500)
                updateNotification()
                return
            }
            if (remainingSeconds <= 0) {
                onTimerFinished()
                return
            }
            remainingSeconds--
            TimerStateHolder.update(remainingSeconds, habitId, systemId, habitTitle, totalSeconds, isPaused)
            updateNotification()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_PAUSE -> {
                isPaused = true
                TimerStateHolder.setPaused(true)
                updateNotification()
                return START_NOT_STICKY
            }
            ACTION_RESUME -> {
                isPaused = false
                TimerStateHolder.setPaused(false)
                updateNotification()
                return START_NOT_STICKY
            }
            ACTION_CANCEL -> {
                cancelAlarm()
                TimerStateHolder.clear()
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
                return START_NOT_STICKY
            }
        }

        habitId = intent?.getLongExtra(EXTRA_HABIT_ID, 0L) ?: 0L
        systemId = intent?.getLongExtra(EXTRA_SYSTEM_ID, 0L) ?: 0L
        habitTitle = intent?.getStringExtra(EXTRA_HABIT_TITLE) ?: ""
        totalSeconds = intent?.getIntExtra(EXTRA_TOTAL_SECONDS, 0)?.coerceIn(1, 99 * 3600) ?: 0
        endTimeMillis = intent?.getLongExtra(EXTRA_END_TIME_MILLIS, 0L) ?: 0L

        if (habitId == 0L || totalSeconds == 0) {
            stopSelf()
            return START_NOT_STICKY
        }

        remainingSeconds = totalSeconds
        isPaused = false
        TimerStateHolder.update(remainingSeconds, habitId, systemId, habitTitle, totalSeconds, false)
        val notification = buildNotification()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        handler.removeCallbacks(tickRunnable)
        handler.post(tickRunnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        handler.removeCallbacks(tickRunnable)
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): android.app.Notification {
        val pauseIntent = Intent(this, TimerForegroundService::class.java).apply { action = ACTION_PAUSE }
        val resumeIntent = Intent(this, TimerForegroundService::class.java).apply { action = ACTION_RESUME }
        val cancelIntent = Intent(this, TimerForegroundService::class.java).apply { action = ACTION_CANCEL }
        val pausePi = PendingIntent.getService(this, 1, pauseIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val resumePi = PendingIntent.getService(this, 2, resumeIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val cancelPi = PendingIntent.getService(this, 3, cancelIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val openAppIntent = PendingIntent.getActivity(
            this, 0,
            packageManager.getLaunchIntentForPackage(packageName),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        val timeStr = "%d:%02d".format(m, s)
        val title = if (isPaused) "Timer paused: $habitTitle" else "Timer: $habitTitle"
        val text = if (isPaused) "$timeStr left" else "$timeStr remaining"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setOngoing(true)
            .setContentIntent(openAppIntent)
            .addAction(android.R.drawable.ic_media_pause, if (isPaused) "Resume" else "Pause", if (isPaused) resumePi else pausePi)
            .addAction(android.R.drawable.ic_delete, "Cancel", cancelPi)
            .build()
    }

    private fun updateNotification() {
        val notification = buildNotification()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).notify(NOTIFICATION_ID, notification)
    }

    private fun onTimerFinished() {
        handler.removeCallbacks(tickRunnable)
        cancelAlarm()
        TimerStateHolder.clear()
        serviceScope.launch {
            withContext(Dispatchers.IO) {
                val app = application as? OnTrackApplication ?: return@withContext
                val todayEpoch = EffectiveDate.todayEpoch()
                val durationMinutes = totalSeconds / 60
                app.database.habitLogDao().completeWithDuration(habitId, todayEpoch, durationMinutes)
                app.streakManager.refreshStreak(systemId)
                val systemIds = app.database.systemDao().getAllSystems().first().filter { !it.isTestData }.map { it.id }
                app.streakManager.refreshGlobalStreak(systemIds)
            }
            showTimerFinishedNotification()
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun showTimerFinishedNotification() {
        com.example.ontrack.util.showTimerFinishedNotification(this, habitTitle)
    }

    private fun cancelAlarm() {
        if (endTimeMillis == 0L) return
        val intent = Intent(this, TimerExpiredReceiver::class.java).apply {
            putExtra(EXTRA_HABIT_ID, habitId)
            putExtra(EXTRA_SYSTEM_ID, systemId)
            putExtra(EXTRA_HABIT_TITLE, habitTitle)
            putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
        }
        val pi = PendingIntent.getBroadcast(this, TIMER_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        (getSystemService(Context.ALARM_SERVICE) as AlarmManager).cancel(pi)
    }

    companion object {
        private const val TIMER_ALARM_REQUEST_CODE = 4000

        fun start(context: Context, habitId: Long, systemId: Long, habitTitle: String, totalSeconds: Int) {
            val endTimeMillis = System.currentTimeMillis() + totalSeconds * 1000L
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_SYSTEM_ID, systemId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
                putExtra(EXTRA_END_TIME_MILLIS, endTimeMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun scheduleExpiredAlarm(context: Context, habitId: Long, systemId: Long, habitTitle: String, totalSeconds: Int) {
            val endTimeMillis = System.currentTimeMillis() + totalSeconds * 1000L
            val intent = Intent(context, TimerExpiredReceiver::class.java).apply {
                putExtra(EXTRA_HABIT_ID, habitId)
                putExtra(EXTRA_SYSTEM_ID, systemId)
                putExtra(EXTRA_HABIT_TITLE, habitTitle)
                putExtra(EXTRA_TOTAL_SECONDS, totalSeconds)
            }
            val pi = PendingIntent.getBroadcast(context, TIMER_ALARM_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pi)
            } else {
                @Suppress("DEPRECATION")
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, endTimeMillis, pi)
            }
        }

        fun pause(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply { action = ACTION_PAUSE }
            context.startService(intent)
        }

        fun resume(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply { action = ACTION_RESUME }
            context.startService(intent)
        }

        fun cancel(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply { action = ACTION_CANCEL }
            context.startService(intent)
        }
    }
}
