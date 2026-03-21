package com.example.ontrack.ui.home

import android.app.Application
import android.widget.Toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.streak.StreakManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flatMapLatest
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.service.TimerStateHolder
import com.example.ontrack.util.EffectiveDate
import com.example.ontrack.util.cancelOngoingTaskNotification
import com.example.ontrack.util.showOngoingTaskNotification
import kotlinx.coroutines.flow.flow

data class TodayTaskItem(
    val habit: com.example.ontrack.data.local.entity.HabitEntity,
    val goalName: String,
    val isCompletedToday: Boolean = false,
    /** First tap on Today; second tap completes (see ongoing notification). */
    val isOngoingToday: Boolean = false,
    val durationMinutes: Int? = null,
    /** How many days this week the habit was completed (for weekly / X per week). */
    val weekCompletionCount: Int = 0
) {
    /** True if weekly target is already reached (e.g. 3/3); can still complete for 4/3, 5/3. */
    fun isWeeklyTargetReached(): Boolean = when (habit.frequencyType) {
        FrequencyType.DAILY -> false
        FrequencyType.WEEKLY -> weekCompletionCount >= 1
        FrequencyType.SPECIFIC_DAYS -> weekCompletionCount >= habit.targetCount
    }
}

/** Days left for a goal with duration; null if no duration or already expired. */
fun SystemEntity.daysLeft(): Int? {
    val dur = duration ?: return null
    val startEpoch = Instant.ofEpochMilli(startDate).atZone(ZoneId.systemDefault()).toLocalDate().toEpochDay()
    val today = EffectiveDate.todayEpoch()
    val left = dur - (today - startEpoch).toInt()
    return if (left > 0) left else null
}

/** Emitted when the Today-page timer reaches zero so UI can show notification and sound. */
data class TodayTimerFinished(val habitTitle: String)

/** Active countdown on Today page (can be paused). */
data class TodayActiveTimer(
    val habitId: Long,
    val systemId: Long,
    val habitTitle: String,
    val totalSeconds: Int,
    val remainingSeconds: Int,
    val isPaused: Boolean
) {
    fun formattedTime(): String {
        val m = remainingSeconds / 60
        val s = remainingSeconds % 60
        return "%d:%02d".format(m, s)
    }
}

@OptIn(ExperimentalCoroutinesApi::class)
class HomeViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val streakManager: StreakManager,
    private val userPreferences: UserPreferences,
    private val application: Application
) : ViewModel() {

    private val _selectedSystemId = MutableStateFlow<Long?>(null)
    /** When non-null, show "goal time has passed" dialog for this system (ID). */
    private val _expiredDialogSystemId = MutableStateFlow<Long?>(null)
    val expiredDialogSystemId: StateFlow<Long?> = _expiredDialogSystemId.asStateFlow()
    val selectedSystemId: StateFlow<Long?> = _selectedSystemId.asStateFlow()

    val systems: StateFlow<List<SystemEntity>> = systemDao.getAllSystems()
        .map { all -> all.filter { !it.isTestData } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _todayCompleteMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val todayCompleteMap: StateFlow<Map<Long, Boolean>> = _todayCompleteMap.asStateFlow()

    private val _freezeCountMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val freezeCountMap: StateFlow<Map<Long, Int>> = _freezeCountMap.asStateFlow()

    /** Consecutive days (counting from today back) where all goals were completed. */
    private val _globalStreakDays = MutableStateFlow(0)
    val globalStreakDays: StateFlow<Int> = _globalStreakDays.asStateFlow()

    /** Global streak: last day that counted as "all goals complete" (UserPreferences). */
    val globalLastStreakDateEpoch: StateFlow<Long> = userPreferences.lastStreakDate
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = -1L
        )

    /** True if today all goals are complete (for drawer header fire/snowflake). */
    val allGoalsCompleteToday: StateFlow<Boolean> = combine(
        _todayCompleteMap,
        systems
    ) { completeMap, list ->
        list.isNotEmpty() && list.all { completeMap[it.id] == true }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false
    )

    /** null = All goals selected; non-null = only these system IDs. */
    private val _todayFilterGoalIds = MutableStateFlow<Set<Long>?>(null)
    val todayFilterGoalIds: StateFlow<Set<Long>?> = _todayFilterGoalIds.asStateFlow()

    private val _selectedDate = MutableStateFlow(EffectiveDate.today())
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val todayEpochDay: Long get() = EffectiveDate.todayEpoch()

    fun setSelectedDate(date: LocalDate) {
        _selectedDate.value = date
    }

    private val dayAndWeekLogsForSelectedDate = _selectedDate.flatMapLatest { date ->
        val dayEpoch = date.toEpochDay()
        val weekStart = date.with(DayOfWeek.MONDAY).toEpochDay()
        val weekEnd = weekStart + 6
        combine(
            habitLogDao.getHabitLogsForDateRange(dayEpoch, dayEpoch),
            habitLogDao.getHabitLogsForDateRange(weekStart, weekEnd)
        ) { dayLogs, weekLogs -> Pair(dayLogs, weekLogs) }
    }

    val todayTasks: StateFlow<List<TodayTaskItem>> = combine(
        habitDao.getAllHabits(),
        systems,
        habitLogDao.getHabitLogsForDateRange(todayEpochDay, todayEpochDay)
    ) { habits, systemsList, todayLogs ->
        val systemIds = systemsList.map { it.id }.toSet()
        habits.filter { it.systemId in systemIds }.map { habit ->
            val goalName = systemsList.find { it.id == habit.systemId }?.goal ?: ""
            val log = todayLogs.find { it.habitId == habit.id }
            val isCompletedToday = log?.isCompleted == true
            val isOngoingToday = log?.isOngoing == true && !isCompletedToday
            val durationMinutes = log?.durationMinutes
            TodayTaskItem(habit, goalName, isCompletedToday, isOngoingToday, durationMinutes)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    val tasksForSelectedDate: StateFlow<List<TodayTaskItem>> = combine(
        habitDao.getAllHabits(),
        systems,
        _selectedDate,
        dayAndWeekLogsForSelectedDate
    ) { habits, systemsList, date, (dayLogs, weekLogs) ->
        val systemIds = systemsList.map { it.id }.toSet()
        buildTasksForDate(date, habits.filter { it.systemId in systemIds }, systemsList, dayLogs, weekLogs)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** Timer state comes from TimerForegroundService via TimerStateHolder (survives app kill). */
    val todayActiveTimer: StateFlow<TodayActiveTimer?> = flow {
        while (true) {
            val h = TimerStateHolder
            emit(
                if (h.isActive) TodayActiveTimer(
                    habitId = h.habitId,
                    systemId = h.systemId,
                    habitTitle = h.habitTitle,
                    totalSeconds = h.totalSeconds,
                    remainingSeconds = h.remainingSeconds,
                    isPaused = h.isPaused
                ) else null
            )
            delay(1000)
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null
    )

    private val _todayTimerFinished = MutableStateFlow<TodayTimerFinished?>(null)
    val todayTimerFinished: StateFlow<TodayTimerFinished?> = _todayTimerFinished.asStateFlow()

    val todayTasksFiltered: StateFlow<List<TodayTaskItem>> = combine(
        tasksForSelectedDate,
        _todayFilterGoalIds,
        userPreferences.todayTaskOrderIds
    ) { tasks, filterIds, orderIds ->
        var list = if (filterIds == null) tasks
        else tasks.filter { it.habit.systemId in filterIds }
        if (orderIds.isNotEmpty()) {
            list = list.sortedBy { task ->
                val i = orderIds.indexOf(task.habit.id)
                if (i < 0) Int.MAX_VALUE else i
            }
        }
        list
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    fun reorderTodayTasks(orderedHabitIds: List<Long>) {
        viewModelScope.launch {
            userPreferences.setTodayTaskOrderIds(orderedHabitIds)
        }
    }

    fun setTodayFilter(goalIds: Set<Long>?) {
        _todayFilterGoalIds.value = goalIds
    }

    fun toggleTodayFilterGoal(systemId: Long) {
        val current = _todayFilterGoalIds.value
        if (current == null) {
            _todayFilterGoalIds.value = setOf(systemId)
        } else {
            val next = if (systemId in current) (current - systemId) else (current + systemId)
            _todayFilterGoalIds.value = if (next.isEmpty()) null else next
        }
    }

    init {
        viewModelScope.launch {
            systemDao.getAllSystems().first().forEach { system ->
                streakManager.refreshStreak(system.id)
            }
        }
        viewModelScope.launch {
            val list = systems.first()
            val shownIds = userPreferences.expiredGoalDialogShownIds.first()
            val expired = list.find { s -> s.duration != null && s.daysLeft() == null && s.id.toString() !in shownIds }
            if (expired != null) _expiredDialogSystemId.value = expired.id
        }
viewModelScope.launch {
                systems.collect { list ->
                    val today = EffectiveDate.todayEpoch()
                _todayCompleteMap.value = list.associate { s ->
                    s.id to streakManager.isDayComplete(s.id, today)
                }
                _freezeCountMap.value = list.associate { s ->
                    s.id to streakManager.getFreezeCount(s.id)
                }
                    streakManager.refreshGlobalStreak(list.map { it.id })
                    _globalStreakDays.value = if (list.isEmpty()) 0 else streakManager.globalStreakFlow().first()
            }
        }
    }

    fun selectSystem(systemId: Long?) {
        _selectedSystemId.value = systemId
    }

    fun deleteSystem(systemId: Long) {
        viewModelScope.launch {
            habitDao.deleteBySystemId(systemId)
            systemDao.deleteById(systemId)
            _selectedSystemId.value = null
        }
    }

    /** Delete multiple goals (habits + systems). */
    fun deleteSystems(systemIds: Collection<Long>) {
        if (systemIds.isEmpty()) return
        viewModelScope.launch {
            systemIds.forEach { id ->
                habitDao.deleteBySystemId(id)
                systemDao.deleteById(id)
            }
            _selectedSystemId.value = null
        }
    }

    fun onExpiredDialogContinue(systemId: Long) {
        viewModelScope.launch {
            userPreferences.addExpiredGoalDialogShown(systemId)
            _expiredDialogSystemId.value = null
        }
    }

    fun onExpiredDialogDelete(systemId: Long) {
        viewModelScope.launch {
            userPreferences.addExpiredGoalDialogShown(systemId)
            deleteSystem(systemId)
            _expiredDialogSystemId.value = null
        }
    }

    fun reorderSystems(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        viewModelScope.launch {
            val list = systems.value.toMutableList()
            val item = list.removeAt(fromIndex)
            list.add(toIndex, item)
            list.forEachIndexed { index, system ->
                if (system.sortOrder != index) {
                    systemDao.updateSystem(system.copy(sortOrder = index))
                }
            }
        }
    }

    /**
     * Second tap on an ongoing Today task (or equivalent): mark done. Done tasks cannot be undone the same day from Today.
     */
    fun onTodayTaskClick(systemId: Long, habitId: Long) {
        viewModelScope.launch {
            val dateEpoch = _selectedDate.value.toEpochDay()
            if (_selectedDate.value != EffectiveDate.today()) return@launch

            val log = habitLogDao.getLog(habitId, dateEpoch)
            val isOngoing = log?.isOngoing == true && log.isCompleted != true
            if (!isOngoing) return@launch

            if (userPreferences.streakSuppressEpochDay.first() == dateEpoch) {
                userPreferences.clearStreakSuppress()
            }
            habitLogDao.completeOngoingAsDone(habitId, dateEpoch)
            streakManager.refreshStreak(systemId)
            refreshTodayComplete()
            cancelOngoingTaskNotification(application, habitId)
        }
    }

    /** After user confirms in the dialog: start ongoing + optional notification. */
    fun confirmStartOngoingTask(systemId: Long, habitId: Long, habitTitle: String) {
        viewModelScope.launch {
            val dateEpoch = _selectedDate.value.toEpochDay()
            if (_selectedDate.value != EffectiveDate.today()) return@launch

            val log = habitLogDao.getLog(habitId, dateEpoch)
            if (log?.isCompleted == true) return@launch
            if (log?.isOngoing == true) return@launch

            val ongoingToday = habitLogDao.getOngoingLogsForDate(dateEpoch)
            val otherOngoing = ongoingToday.any { it.habitId != habitId }
            if (otherOngoing) {
                Toast.makeText(
                    application,
                    "Finish your current task before starting another.",
                    Toast.LENGTH_SHORT
                ).show()
                return@launch
            }
            habitLogDao.markOngoing(habitId, dateEpoch)
            if (userPreferences.notificationsEnabled.first()) {
                showOngoingTaskNotification(
                    application,
                    habitId,
                    systemId,
                    habitTitle,
                    dateEpoch
                )
            }
        }
    }

    /** Call when returning to Home so card colors and freeze counts are up to date. */
    fun refreshTodayComplete() {
        viewModelScope.launch {
            val list = systems.value
            val today = EffectiveDate.todayEpoch()
            _todayCompleteMap.value = list.associate { s ->
                s.id to streakManager.isDayComplete(s.id, today)
            }
            _freezeCountMap.value = list.associate { s ->
                s.id to streakManager.getFreezeCount(s.id)
            }
            streakManager.refreshGlobalStreak(list.map { it.id })
            _globalStreakDays.value = if (list.isEmpty()) 0 else streakManager.globalStreakFlow().first()
        }
    }

    /** Timer is started by the Activity (TimerForegroundService); this is a no-op for compatibility. */
    fun startTimerFromToday(systemId: Long, habitId: Long, habitTitle: String, totalSeconds: Int) {
        // Service is started from Activity; state is read from TimerStateHolder via todayActiveTimer flow
    }

    fun clearTodayTimerFinished() {
        _todayTimerFinished.value = null
    }

    /** Pause is sent to TimerForegroundService from Activity. */
    fun pauseTodayTimer() {}

    /** Resume is sent to TimerForegroundService from Activity. */
    fun resumeTodayTimer() {}

    fun saveLastTimerDuration(habitId: Long, totalSeconds: Int) {
        viewModelScope.launch {
            habitDao.updateLastTimerDuration(habitId, totalSeconds)
        }
    }

    private fun buildTasksForDate(
        date: LocalDate,
        habits: List<com.example.ontrack.data.local.entity.HabitEntity>,
        systemsList: List<SystemEntity>,
        dayLogs: List<HabitLogEntity>,
        weekLogs: List<HabitLogEntity>
    ): List<TodayTaskItem> {
        val today = EffectiveDate.today()
        val weekCompletedByHabit = weekLogs
            .filter { it.isCompleted }
            .groupBy { it.habitId }
            .mapValues { (_, logs) -> logs.distinctBy { it.date }.size }

        return when {
            date.isBefore(today) -> {
                dayLogs
                    .filter { it.isCompleted }
                    .mapNotNull { log ->
                        val habit = habits.find { it.id == log.habitId } ?: return@mapNotNull null
                        val goalName = systemsList.find { it.id == habit.systemId }?.goal ?: ""
                        val wc = weekCompletedByHabit[habit.id] ?: 0
                        TodayTaskItem(
                            habit,
                            goalName,
                            isCompletedToday = true,
                            isOngoingToday = false,
                            durationMinutes = log.durationMinutes,
                            weekCompletionCount = wc
                        )
                    }
            }
            date.isAfter(today) -> {
                habits
                    .filter { habit ->
                        when (habit.frequencyType) {
                            FrequencyType.DAILY -> true
                            FrequencyType.WEEKLY -> (weekCompletedByHabit[habit.id] ?: 0) == 0
                            FrequencyType.SPECIFIC_DAYS -> (weekCompletedByHabit[habit.id] ?: 0) < habit.targetCount
                        }
                    }
                    .map { habit ->
                        val goalName = systemsList.find { it.id == habit.systemId }?.goal ?: ""
                        val wc = weekCompletedByHabit[habit.id] ?: 0
                        TodayTaskItem(
                            habit,
                            goalName,
                            isCompletedToday = false,
                            isOngoingToday = false,
                            durationMinutes = null,
                            weekCompletionCount = wc
                        )
                    }
            }
            else -> {
                val list = habits
                    .filter { habit ->
                        val wc = weekCompletedByHabit[habit.id] ?: 0
                        when (habit.frequencyType) {
                            FrequencyType.DAILY -> true
                            FrequencyType.WEEKLY -> wc < 1
                            FrequencyType.SPECIFIC_DAYS -> wc < habit.targetCount
                        }
                    }
                    .map { habit ->
                        val goalName = systemsList.find { it.id == habit.systemId }?.goal ?: ""
                        val log = dayLogs.find { it.habitId == habit.id }
                        val wc = weekCompletedByHabit[habit.id] ?: 0
                        val completed = log?.isCompleted == true
                        TodayTaskItem(
                            habit,
                            goalName,
                            isCompletedToday = completed,
                            isOngoingToday = log?.isOngoing == true && !completed,
                            durationMinutes = log?.durationMinutes,
                            weekCompletionCount = wc
                        )
                    }
                list.sortedBy { item -> if (item.isWeeklyTargetReached()) 1 else 0 }
            }
        }
    }
}
