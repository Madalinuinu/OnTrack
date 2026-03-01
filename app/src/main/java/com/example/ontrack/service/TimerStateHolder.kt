package com.example.ontrack.service

/**
 * Shared state for the timer foreground service so the UI (ViewModels) can display
 * the current countdown when the app is in foreground.
 */
object TimerStateHolder {

    @Volatile
    var remainingSeconds: Int = 0
        private set

    @Volatile
    var habitId: Long = 0L
        private set

    @Volatile
    var systemId: Long = 0L
        private set

    @Volatile
    var habitTitle: String = ""
        private set

    @Volatile
    var totalSeconds: Int = 0
        private set

    @Volatile
    var isPaused: Boolean = false
        private set

    /** True when the timer is running (service is active). */
    @Volatile
    var isActive: Boolean = false
        private set

    fun update(
        remainingSeconds: Int,
        habitId: Long,
        systemId: Long,
        habitTitle: String,
        totalSeconds: Int,
        isPaused: Boolean
    ) {
        this.remainingSeconds = remainingSeconds
        this.habitId = habitId
        this.systemId = systemId
        this.habitTitle = habitTitle
        this.totalSeconds = totalSeconds
        this.isPaused = isPaused
        this.isActive = true
    }

    fun clear() {
        isActive = false
        remainingSeconds = 0
        habitId = 0L
        systemId = 0L
        habitTitle = ""
        totalSeconds = 0
        isPaused = false
    }

    fun setPaused(paused: Boolean) {
        isPaused = paused
    }
}
