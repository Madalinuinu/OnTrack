package com.example.ontrack

/**
 * Holder for "start timer" request from Today page.
 * TrackerScreen reads and consumes this when it opens.
 */
data class PendingTimerStart(
    val systemId: Long,
    val habitId: Long,
    val habitTitle: String,
    val totalSeconds: Int
)

object PendingTimer {
    @Volatile
    var data: PendingTimerStart? = null
        private set

    fun set(systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) {
        data = PendingTimerStart(systemId, habitId, habitTitle, totalSeconds)
    }

    fun consume(): PendingTimerStart? {
        val d = data
        data = null
        return d
    }
}
