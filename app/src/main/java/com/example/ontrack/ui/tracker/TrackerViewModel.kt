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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate

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
    private val systemId: Long
) : ViewModel() {

    private val _system = MutableStateFlow<SystemEntity?>(null)
    val system: StateFlow<SystemEntity?> = _system.asStateFlow()

    private val todayEpochDay: Long = LocalDate.now().toEpochDay()
    private val weekStartEpochDay: Long = LocalDate.now().with(DayOfWeek.MONDAY).toEpochDay()
    private val weekEndEpochDay: Long = weekStartEpochDay + 6

    val currentStreak: StateFlow<Int> = streakManager.currentStreak
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    val trackerItems: StateFlow<List<TrackerItem>> = combine(
        habitDao.getHabitsForSystem(systemId),
        habitLogDao.getHabitLogsForDateRange(todayEpochDay, todayEpochDay),
        habitLogDao.getHabitLogsForDateRange(weekStartEpochDay, weekEndEpochDay)
    ) { habits, todayLogs, weekLogs ->
        buildTrackerItems(habits, todayLogs, weekLogs)
    }.stateIn(
        scope = viewModelScope,
        started = kotlinx.coroutines.flow.SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    init {
        viewModelScope.launch {
            _system.value = systemDao.getSystemById(systemId)
        }
    }

    fun toggleHabit(habitId: Long) {
        viewModelScope.launch {
            habitLogDao.toggleHabitCompletion(habitId, todayEpochDay)
            streakManager.refreshStreak()
        }
    }

    private fun buildTrackerItems(
        habits: List<HabitEntity>,
        todayLogs: List<HabitLogEntity>,
        weekLogs: List<HabitLogEntity>
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
                val isCompletedToday = todayByHabit.contains(habit.id)
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
