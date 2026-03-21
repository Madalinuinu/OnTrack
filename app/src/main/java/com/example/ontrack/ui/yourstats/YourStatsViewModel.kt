package com.example.ontrack.ui.yourstats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.data.streak.StreakManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import com.example.ontrack.util.EffectiveDate
import java.time.LocalDate
import java.time.format.DateTimeFormatter

enum class StatsRange(val label: String) {
    LAST_7_DAYS("Last 7 days"),
    LAST_MONTH("Last month"),
    LAST_YEAR("Last year")
}

data class GoalTimeItem(
    val system: SystemEntity,
    val totalMinutes: Int
)

data class DayTotalItem(
    val epochDay: Long,
    val label: String,
    val totalMinutes: Int
)

data class YourStatsUiState(
    val goalTimeItems: List<GoalTimeItem> = emptyList(),
    val chartData: List<DayTotalItem> = emptyList(),
    val selectedRange: StatsRange = StatsRange.LAST_7_DAYS,
    val completedEpochDays: Set<Long> = emptySet(),
    /** First day ever completed (kept for potential future use). */
    val firstCompletedEpoch: Long? = null,
    /** First day when app started being used (min system.startDate as epochDay). */
    val firstUsageEpoch: Long? = null,
    val pausedEpochDays: Set<Long> = emptySet(),
    val todayEpoch: Long = 0L,
    val totalDaysCompleted: Int = 0,
    /** Consecutive days (from today back) when all goals were completed. Same logic as drawer. */
    val globalStreakDays: Int = 0,
    /** True if today all goals are complete (for fire/ice display). */
    val allGoalsCompleteToday: Boolean = false,
    /** Global streak last date (UserPreferences), for header display when today incomplete. */
    val globalLastStreakDateEpoch: Long = -1L,
    /** True when vacation mode is on and today is in vacation range; streak shows orange (frozen). */
    val isVacationDay: Boolean = false,
    val isLoading: Boolean = true,
    /** For day-detail sheet: goals with their habits; empty when no day selected. */
    val goalsWithHabits: List<Pair<SystemEntity, List<HabitEntity>>> = emptyList(),
    val allLogs: List<HabitLogEntity> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class)
class YourStatsViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(YourStatsUiState())
    val uiState: StateFlow<YourStatsUiState> = _uiState.asStateFlow()

    private val _selectedRange = MutableStateFlow(StatsRange.LAST_7_DAYS)
    fun setRange(range: StatsRange) { _selectedRange.value = range }

    private data class StatsData(
        val goalTimeItems: List<GoalTimeItem>,
        val completedEpochDays: Set<Long>,
        val pausedEpochDays: Set<Long>,
        val todayEpoch: Long,
        val isVacationDay: Boolean,
        val globalStreakDays: Int,
        /** UserPreferences last streak date; for header ice number when today toggled incomplete. */
        val globalLastStreakDateEpoch: Long,
        val habitIdsFiltered: List<Long>,
        val logs: List<HabitLogEntity>,
        val goalsWithHabits: List<Pair<SystemEntity, List<HabitEntity>>>,
        val firstUsageEpoch: Long?
    )

    init {
        viewModelScope.launch {
            combine(
                combine(
                combine(
                    systemDao.getAllSystems().map { systems -> systems.filter { !it.isTestData } },
                    habitDao.getAllHabits(),
                    habitLogDao.getHabitLogsForDateRange(0L, 100_000L),
                    userPreferences.vacationModeEnabled,
                    userPreferences.vacationModeFromEpochDay
                ) { systemsFiltered, habits, logs, vacationOn, vacationFrom ->
                    val todayEpoch = EffectiveDate.todayEpoch()
                    val isVacationDay = vacationOn && vacationFrom >= 0 && todayEpoch >= vacationFrom
                    val habitsBySystem = habits.groupBy { it.systemId }
                    val logsByDate = logs.filter { it.isCompleted }.groupBy { it.date }
                    val startEpoch = systemsFiltered.minOfOrNull { sys ->
                        java.time.Instant.ofEpochMilli(sys.startDate)
                            .atZone(java.time.ZoneId.systemDefault())
                            .toLocalDate()
                            .toEpochDay()
                    }
                    val goalsWithHabits = systemsFiltered.map { s ->
                        s to (habitsBySystem[s.id] ?: emptyList())
                    }
                    val pausedEpochDays = if (vacationOn && vacationFrom >= 0) {
                        (vacationFrom..todayEpoch).toSet()
                    } else emptySet()
                    val logMinutesByHabit = logs
                        .filter { it.isCompleted && (it.durationMinutes ?: 0) > 0 }
                        .groupBy { it.habitId }
                        .mapValues { (_, list) -> list.sumOf { it.durationMinutes ?: 0 } }
                    val goalTimeItems = systemsFiltered.map { system ->
                        val systemHabits = habitsBySystem[system.id] ?: emptyList()
                        val total = systemHabits.sumOf { habit -> logMinutesByHabit[habit.id] ?: 0 }
                        GoalTimeItem(system = system, totalMinutes = total)
                    }.sortedByDescending { it.totalMinutes }
                    val systemIdsSet = systemsFiltered.map { it.id }.toSet()
                    val habitIdsFiltered = habits.filter { it.systemId in systemIdsSet }.map { it.id }
                    StatsData(
                        goalTimeItems = goalTimeItems,
                        completedEpochDays = emptySet(), // filled in flatMapLatest via StreakManager
                        pausedEpochDays = pausedEpochDays,
                        todayEpoch = todayEpoch,
                        isVacationDay = isVacationDay,
                        globalStreakDays = 0, // filled in flatMapLatest after refreshGlobalStreak
                        globalLastStreakDateEpoch = -1L,
                        habitIdsFiltered = habitIdsFiltered,
                        logs = logs,
                        goalsWithHabits = goalsWithHabits,
                        firstUsageEpoch = startEpoch
                    )
                },
                userPreferences.persistedVacationEpochDays
            ) { data, persisted ->
                data.copy(pausedEpochDays = data.pausedEpochDays + persisted)
            }.flatMapLatest { data ->
                flow {
                    val systems = data.goalTimeItems.map { it.system }
                    val rangeStart = data.firstUsageEpoch
                        ?: data.goalTimeItems.minOfOrNull { g ->
                            java.time.Instant.ofEpochMilli(g.system.startDate)
                                .atZone(java.time.ZoneId.systemDefault()).toLocalDate().toEpochDay()
                        } ?: data.todayEpoch
                    val rangeStartBounded = maxOf(rangeStart, data.todayEpoch - 365)
                    val completed = mutableSetOf<Long>()
                    for (day in rangeStartBounded..data.todayEpoch) {
                        if (systems.isNotEmpty()) {
                            var allComplete = true
                            for (s in systems) {
                                if (!streakManager.isDayComplete(s.id, day)) {
                                    allComplete = false
                                    break
                                }
                            }
                            if (allComplete) completed.add(day)
                        }
                    }
                    streakManager.refreshGlobalStreak(systems.map { it.id })
                    val globalStreakDays = if (systems.isEmpty()) 0 else streakManager.globalStreakFlow().first()
                    val globalLastStreakDateEpoch = userPreferences.lastStreakDate.first()
                    emit(
                        data.copy(
                            completedEpochDays = completed,
                            globalStreakDays = globalStreakDays,
                            globalLastStreakDateEpoch = globalLastStreakDateEpoch
                        )
                    )
                }
            },
                _selectedRange
            ) { data, selectedRange ->
                val chartData = buildChartData(
                    selectedRange = selectedRange,
                    todayEpoch = data.todayEpoch,
                    logs = data.logs,
                    habitIdsFiltered = data.habitIdsFiltered,
                    completedEpochDays = data.completedEpochDays
                )
                val allGoalsCompleteToday = data.todayEpoch in data.completedEpochDays
                val firstCompletedEpoch = data.completedEpochDays.minOrNull()
                YourStatsUiState(
                    goalTimeItems = data.goalTimeItems,
                    chartData = chartData,
                    selectedRange = selectedRange,
                    completedEpochDays = data.completedEpochDays,
                    firstCompletedEpoch = firstCompletedEpoch,
                    firstUsageEpoch = data.firstUsageEpoch,
                    pausedEpochDays = data.pausedEpochDays,
                    todayEpoch = data.todayEpoch,
                    totalDaysCompleted = data.completedEpochDays.size,
                    globalStreakDays = data.globalStreakDays,
                    allGoalsCompleteToday = allGoalsCompleteToday,
                    globalLastStreakDateEpoch = data.globalLastStreakDateEpoch,
                    isVacationDay = data.isVacationDay,
                    isLoading = false,
                    goalsWithHabits = data.goalsWithHabits,
                    allLogs = data.logs
                )
            }.collect { _uiState.value = it }
        }
    }

    private fun buildChartData(
        selectedRange: StatsRange,
        todayEpoch: Long,
        logs: List<HabitLogEntity>,
        habitIdsFiltered: List<Long>,
        completedEpochDays: Set<Long>
    ): List<DayTotalItem> {
        val formatterShort = DateTimeFormatter.ofPattern("d MMM")
        val formatterMonth = DateTimeFormatter.ofPattern("MMM")
        val dayLogs = logs
            .filter { it.isCompleted && it.habitId in habitIdsFiltered }
            .groupBy { it.date }
            .mapValues { (_, list) -> list.sumOf { it.durationMinutes ?: 0 } }
        return when (selectedRange) {
            StatsRange.LAST_7_DAYS -> {
                (0..6).map { offset ->
                    val day = todayEpoch - (6 - offset)
                    val date = LocalDate.ofEpochDay(day)
                    DayTotalItem(
                        epochDay = day,
                        label = date.format(DateTimeFormatter.ofPattern("EEE")),
                        totalMinutes = dayLogs[day] ?: 0
                    )
                }
            }
            StatsRange.LAST_MONTH -> {
                (0 until 30).map { offset ->
                    val day = todayEpoch - (29 - offset)
                    val date = LocalDate.ofEpochDay(day)
                    DayTotalItem(
                        epochDay = day,
                        label = date.format(formatterShort),
                        totalMinutes = dayLogs[day] ?: 0
                    )
                }
            }
            StatsRange.LAST_YEAR -> {
                val today = LocalDate.ofEpochDay(todayEpoch)
                (0 until 12).map { monthOffset ->
                    val month = today.minusMonths(11L - monthOffset)
                    val start = month.withDayOfMonth(1).toEpochDay()
                    val end = month.withDayOfMonth(month.lengthOfMonth()).toEpochDay().coerceAtMost(todayEpoch)
                    val totalMinutes = (start..end).sumOf { d -> dayLogs[d] ?: 0 }
                    DayTotalItem(
                        epochDay = start,
                        label = month.format(formatterMonth),
                        totalMinutes = totalMinutes
                    )
                }
            }
        }
    }
}

class YourStatsViewModelFactory(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences,
    private val streakManager: StreakManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != YourStatsViewModel::class.java) throw IllegalArgumentException("Unknown ViewModel")
        return YourStatsViewModel(systemDao, habitDao, habitLogDao, userPreferences, streakManager) as T
    }
}
