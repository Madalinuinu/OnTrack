package com.example.ontrack.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.ontrack.MainActivity
import com.example.ontrack.receiver.OngoingTaskReceiver

const val EXTRA_SCROLL_TO_TODAY = "scroll_to_today"

private const val CHANNEL_ID = "ontrack_ongoing_task"
private const val CHANNEL_ID_SILENT = "ontrack_ongoing_task_silent"
private const val REQ_CONTENT = 3000
private const val REQ_FINISH = 4000
private const val REQ_OPEN = 5000

fun ongoingTaskNotificationId(habitId: Long): Int {
    val h = habitId.hashCode()
    return 20_000 + (h and 0x7fff)
}

fun cancelOngoingTaskNotification(context: Context, habitId: Long) {
    try {
        NotificationManagerCompat.from(context).cancel(ongoingTaskNotificationId(habitId))
    } catch (_: Exception) { }
}

/**
 * Persists until the task is marked done in-app or via "Mark as finished".
 * Tap / "Open in app" only opens the app (Today tab); they do not complete the habit.
 */
fun showOngoingTaskNotification(
    context: Context,
    habitId: Long,
    systemId: Long,
    habitTitle: String,
    dateEpoch: Long,
    soundEnabled: Boolean
) {
    val channelId = if (soundEnabled) CHANNEL_ID else CHANNEL_ID_SILENT
    val channel = NotificationChannel(
        channelId,
        "Ongoing tasks",
        NotificationManager.IMPORTANCE_HIGH
    ).apply {
        setShowBadge(true)
        enableVibration(soundEnabled)
        if (soundEnabled) {
            setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION), null)
        } else {
            setSound(null, null)
        }
    }
    (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
        .createNotificationChannel(channel)

    val nid = ongoingTaskNotificationId(habitId)
    val flags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE

    fun openAppIntent() = Intent(context, MainActivity::class.java).apply {
        this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        putExtra("start_page", 1)
        putExtra(EXTRA_SCROLL_TO_TODAY, true)
    }

    val contentPi = PendingIntent.getActivity(context, REQ_CONTENT + nid, openAppIntent(), flags)
    val openPi = PendingIntent.getActivity(context, REQ_OPEN + nid, openAppIntent(), flags)

    val finishIntent = Intent(context, OngoingTaskReceiver::class.java).apply {
        action = OngoingTaskReceiver.ACTION_MARK_FINISHED
        putExtra(OngoingTaskReceiver.EXTRA_HABIT_ID, habitId)
        putExtra(OngoingTaskReceiver.EXTRA_SYSTEM_ID, systemId)
        putExtra(OngoingTaskReceiver.EXTRA_DATE_EPOCH, dateEpoch)
    }
    val finishPi = PendingIntent.getBroadcast(context, REQ_FINISH + nid, finishIntent, flags)

    val text =
        "This task \"$habitTitle\" is ongoing. Tap to open in app."

    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_popup_reminder)
        .setContentTitle("Ongoing")
        .setContentText(text)
        .setStyle(NotificationCompat.BigTextStyle().bigText(text))
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setCategory(NotificationCompat.CATEGORY_PROGRESS)
        .setOngoing(true)
        .setAutoCancel(false)
        .setOnlyAlertOnce(true)
        .setContentIntent(contentPi)
        .addAction(0, "Mark as finished", finishPi)
        .addAction(0, "Open in app", openPi)
        .setSilent(!soundEnabled)
        .setSound(
            if (soundEnabled) RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION) else null
        )
        .build()

    try {
        NotificationManagerCompat.from(context).notify(nid, notification)
    } catch (_: Exception) { }
}
