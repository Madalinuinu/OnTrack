package com.example.ontrack.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.data.streak.StreakManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Calendar
import java.time.LocalDate

/** Calendar state: completed days, paused days (orange), today, totals, streak, month navigation. */
data class ActivityUiState(
    val systemGoal: String = "",
    val todayEpoch: Long = 0L,
    val completedEpochDays: Set<Long> = emptySet(),
    val pausedEpochDays: Set<Long> = emptySet(),
    val totalDaysCompleted: Int = 0,
    val currentStreak: Int = 0,
    val freezeCount: Int = 0,
    val isTodayComplete: Boolean = false,
    val isLoading: Boolean = true,
    val habits: List<HabitEntity> = emptyList(),
    val logs: List<HabitLogEntity> = emptyList(),
    /** Total minutes per habit (for progress section). */
    val totalMinutesByHabitId: Map<Long, Int> = emptyMap()
)

class ActivityViewModel(
    private val systemId: Long,
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityUiState())
    val uiState: StateFlow<ActivityUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            loadActivity()
        }
    }

    /** Reload calendar, logs and progress (e.g. after returning from Tracker). */
    fun refresh() {
        viewModelScope.launch {
            loadActivity()
        }
    }

    /** Today as LocalDate without using LocalDate.ofInstant (API 24 compatible). */
    private fun todayLocalDate(): LocalDate {
        val c = Calendar.getInstance()
        return LocalDate.of(
            c.get(Calendar.YEAR),
            c.get(Calendar.MONTH) + 1,
            c.get(Calendar.DAY_OF_MONTH)
        )
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

    private suspend fun loadActivity() {
        val system = systemDao.getSystemById(systemId) ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false)
            return
        }
        val habits = habitDao.getHabitsForSystem(systemId).first()
        val today = todayLocalDate()
        val todayEpoch = today.toEpochDay()
        val startDate = millisToLocalDate(system.startDate)
        val startEpoch = startDate.toEpochDay()
        val endEpoch = todayEpoch

        val pausedEpochDays = mutableSetOf<Long>()
        val from = system.pausedFromEpochDay
        val to = system.pausedToEpochDay
        if (from != null && to != null) {
            for (d in minOf(from, to)..maxOf(from, to)) {
                pausedEpochDays.add(d)
            }
        }

        val logs = habitLogDao.getHabitLogsForDateRange(startEpoch, endEpoch).first()
        val logsByDate = logs.groupBy { it.date }
        val totalMinutesByHabitId = logs
            .filter { it.isCompleted }
            .groupBy { it.habitId }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes ?: 0 } }

        if (habits.isEmpty()) {
            _uiState.value = ActivityUiState(
                systemGoal = system.goal,
                todayEpoch = todayEpoch,
                pausedEpochDays = pausedEpochDays,
                totalDaysCompleted = 0,
                currentStreak = system.currentStreak,
                freezeCount = streakManager.getFreezeCount(systemId),
                isTodayComplete = false,
                isLoading = false,
                habits = emptyList(),
                logs = logs,
                totalMinutesByHabitId = totalMinutesByHabitId
            )
            return
        }

        val completedEpochDays = (startEpoch..endEpoch).filter { epochDay ->
            val dayLogs = logsByDate[epochDay]?.associate { it.habitId to it.isCompleted } ?: emptyMap()
            habits.all { habit -> dayLogs[habit.id] == true }
        }.toSet()

        val isTodayComplete = streakManager.isDayComplete(systemId, todayEpoch)
        val freezeCount = streakManager.getFreezeCount(systemId)

        _uiState.value = ActivityUiState(
            systemGoal = system.goal,
            todayEpoch = todayEpoch,
            completedEpochDays = completedEpochDays,
            pausedEpochDays = pausedEpochDays,
            totalDaysCompleted = completedEpochDays.size,
            currentStreak = system.currentStreak,
            freezeCount = freezeCount,
            isTodayComplete = isTodayComplete,
            isLoading = false,
            habits = habits,
            logs = logs,
            totalMinutesByHabitId = totalMinutesByHabitId
        )
    }
}

class ActivityViewModelFactory(
    private val systemId: Long,
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val streakManager: StreakManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != ActivityViewModel::class.java) throw IllegalArgumentException("Unknown ViewModel")
        return ActivityViewModel(systemId, systemDao, habitDao, habitLogDao, streakManager) as T
    }
}
