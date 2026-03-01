package com.example.ontrack.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.streak.StreakManager
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ontrack.service.TimerStateHolder
import com.example.ontrack.util.EffectiveDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.util.Calendar
import kotlin.math.max
import kotlinx.coroutines.flow.flow

/** Active countdown for one habit. */
data class ActiveTimer(
    val habitId: Long,
    val habitTitle: String,
    val remainingSeconds: Int,
    val totalSeconds: Int
) {
    fun formattedTime(): String {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return "%d:%02d".format(m, s)
    }
}

/** Emitted when timer reaches 0 so UI can play sound, show notification, then mark complete. */
data class TimerFinished(
    val habitId: Long,
    val habitTitle: String,
    val durationMinutes: Int
)

/** One row in the tracker: habit + today's completion + optional week progress. */
data class TrackerItem(
    val habit: HabitEntity,
    val isCompletedToday: Boolean,
    /** For WEEKLY: 0 or 1. For SPECIFIC_DAYS: count this week. For DAILY: 0. */
    val completionsThisWeek: Int
) {
    /** Subtitle for WEEKLY / SPECIFIC_DAYS (e.g. "1/3 done this week"). */
    fun progressSubtitle(): String? = when (habit.frequencyType) {
        FrequencyType.DAILY -> null
        FrequencyType.WEEKLY -> if (completionsThisWeek >= 1) "Done this week" else "Once this week"
        FrequencyType.SPECIFIC_DAYS -> "${completionsThisWeek}/${habit.targetCount} done this week"
    }
}

class TrackerViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val streakManager: StreakManager,
    private val userPreferences: UserPreferences,
    val systemId: Long
) : ViewModel() {

    private val _system = MutableStateFlow<SystemEntity?>(null)
    val system: StateFlow<SystemEntity?> = _system.asStateFlow()

    private val _daysLeft = MutableStateFlow<Int?>(null)
    /** Number of days left until system end (when duration is set); null if no term limit. */
    val daysLeft: StateFlow<Int?> = _daysLeft.asStateFlow()

    private val todayEpochDay: Long get() = EffectiveDate.todayEpoch()
    private val weekStartEpochDay: Long get() = EffectiveDate.today().with(DayOfWeek.MONDAY).toEpochDay()
    private val weekEndEpochDay: Long get() = weekStartEpochDay + 6

    val currentStreak: StateFlow<Int> = streakManager.currentStreakFlow(systemId)
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    private val _isTodayComplete = MutableStateFlow(false)
    val isTodayComplete: StateFlow<Boolean> = _isTodayComplete.asStateFlow()

    private val _freezeCount = MutableStateFlow(0)
    val freezeCount: StateFlow<Int> = _freezeCount.asStateFlow()

    val isVacationDay: StateFlow<Boolean> = combine(
        userPreferences.vacationModeEnabled,
        userPreferences.vacationModeFromEpochDay
    ) { vacationOn, vacationFrom ->
        val today = EffectiveDate.todayEpoch()
        vacationOn && vacationFrom >= 0 && today >= vacationFrom
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    /** Timer state comes from TimerForegroundService via TimerStateHolder (survives app kill). */
    val activeTimer: StateFlow<ActiveTimer?> = flow {
        while (true) {
            val h = TimerStateHolder
            emit(
                if (h.isActive) ActiveTimer(
                    habitId = h.habitId,
                    habitTitle = h.habitTitle,
                    remainingSeconds = h.remainingSeconds,
                    totalSeconds = h.totalSeconds
                ) else null
            )
            kotlinx.coroutines.delay(1000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _timerFinished = MutableStateFlow<TimerFinished?>(null)
    val timerFinished: StateFlow<TimerFinished?> = _timerFinished.asStateFlow()

    /** Habit IDs we just completed via timer; keep showing green until DB flow catches up. */
    private val _justCompletedHabitIds = MutableStateFlow<Set<Long>>(emptySet())

    val trackerItems: StateFlow<List<TrackerItem>> = combine(
        habitDao.getHabitsForSystem(systemId),
        habitLogDao.getHabitLogsForDateRange(todayEpochDay, todayEpochDay),
        habitLogDao.getHabitLogsForDateRange(weekStartEpochDay, weekEndEpochDay),
        _justCompletedHabitIds
    ) { habits, todayLogs, weekLogs, justCompleted ->
        val todayByHabit = todayLogs.filter { it.isCompleted }.map { it.habitId }.toSet()
        val toRemove = justCompleted.filter { it in todayByHabit }
        if (toRemove.isNotEmpty()) {
            viewModelScope.launch {
                _justCompletedHabitIds.value = _justCompletedHabitIds.value - toRemove.toSet()
            }
        }
        buildTrackerItems(habits, todayLogs, weekLogs, justCompleted)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            val sys = systemDao.getSystemById(systemId)
            _system.value = sys
            sys?.let { s ->
                if (s.duration != null) {
                    val startDate = millisToLocalDate(s.startDate)
                    val startEpoch = startDate.toEpochDay()
                    val endEpoch = startEpoch + s.duration - 1
                    _daysLeft.value = max(0, (endEpoch - todayEpochDay).toInt())
                } else {
                    _daysLeft.value = null
                }
            } ?: run { _daysLeft.value = null }
            streakManager.refreshStreak(systemId)
            _isTodayComplete.value = streakManager.isDayComplete(systemId, todayEpochDay)
            _freezeCount.value = streakManager.getFreezeCount(systemId)
        }
    }

    /** Millis to LocalDate without using ofInstant (API 24 compatible). */
    private fun millisToLocalDate(millis: Long): LocalDate {
        val c = Calendar.getInstance()
        c.timeInMillis = millis
        return LocalDate.of(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitLogDao.toggleHabitCompletion(habitId, todayEpochDay)
            streakManager.refreshStreak(systemId)
            _isTodayComplete.value = streakManager.isDayComplete(systemId, todayEpochDay)
            _freezeCount.value = streakManager.getFreezeCount(systemId)
        }
    }

    /** Timer is started by the Activity (TimerForegroundService); this is a no-op for compatibility. */
    fun startTimer(habitId: Long, habitTitle: String, totalSeconds: Int) {
        // Service is started from Activity; state is read from TimerStateHolder via activeTimer flow
    }

    fun cancelTimer() {
        // Cancel is sent to TimerForegroundService from Activity if needed
    }

    fun clearTimerFinished() {
        _timerFinished.value = null
    }

    fun saveLastTimerDuration(habitId: Long, totalSeconds: Int) {
        viewModelScope.launch {
            habitDao.updateLastTimerDuration(habitId, totalSeconds)
        }
    }

    private fun buildTrackerItems(
        habits: List<HabitEntity>,
        todayLogs: List<HabitLogEntity>,
        weekLogs: List<HabitLogEntity>,
        justCompletedIds: Set<Long> = emptySet()
    ): List<TrackerItem> {
        val todayByHabit = todayLogs.filter { it.isCompleted }.map { it.habitId }.toSet()
        val weekCompletedByHabit = weekLogs
            .filter { it.isCompleted }
            .groupBy { it.habitId }
            .mapValues { (_, logs) -> logs.distinctBy { it.date }.size }

        return habits
            .filter { habit ->
                when (habit.frequencyType) {
                    FrequencyType.DAILY, FrequencyType.SPECIFIC_DAYS -> true
                    FrequencyType.WEEKLY -> (weekCompletedByHabit[habit.id] ?: 0) == 0
                }
            }
            .map { habit ->
                val isCompletedToday = todayByHabit.contains(habit.id) || justCompletedIds.contains(habit.id)
                val completionsThisWeek = when (habit.frequencyType) {
                    FrequencyType.DAILY -> 0
                    FrequencyType.WEEKLY, FrequencyType.SPECIFIC_DAYS -> weekCompletedByHabit[habit.id] ?: 0
                }
                TrackerItem(
                    habit = habit,
                    isCompletedToday = isCompletedToday,
                    completionsThisWeek = completionsThisWeek
                )
            }
    }
}
