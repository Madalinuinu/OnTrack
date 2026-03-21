package com.example.ontrack.ui.editsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.streak.StreakManager
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.ui.createsystem.HabitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.example.ontrack.util.EffectiveDate
import java.time.LocalDate

data class EditSystemUiState(
    val systemId: Long = 0L,
    val goal: String = "",
    val duration: String = "",
    val habits: List<HabitItem> = emptyList(),
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val navigateBack: Boolean = false
)

class EditSystemViewModel(
    private val systemId: Long,
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val userPreferences: UserPreferences,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSystemUiState(systemId = systemId))
    val uiState: StateFlow<EditSystemUiState> = _uiState.asStateFlow()

    /** Stable negative ids for newly added habits in edit so reorderable list keys don't conflict. */
    private var nextTempId = -1L

    private var cachedEditTodayEpochDay: Long = -1L
    private var cachedEditTodayComplete: Boolean = false

    init {
        viewModelScope.launch {
            val today = EffectiveDate.todayEpoch()
            cachedEditTodayEpochDay = today
            cachedEditTodayComplete = streakManager.isDayComplete(systemId, today)
            // Prevent streak/calandar day status from changing due to habit edits.
            userPreferences.setStreakSuppressForSystem(systemId, today, cachedEditTodayComplete)
            load()
        }
    }

    private suspend fun load() {
        val system = systemDao.getSystemById(systemId) ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false, navigateBack = true)
            return
        }
        val habits = habitDao.getHabitsForSystem(systemId).first().map { h ->
            HabitItem(
                id = h.id,
                title = h.title,
                frequencyType = h.frequencyType,
                targetCount = h.targetCount,
                trackTimeEnabled = false
            )
        }
        _uiState.value = EditSystemUiState(
            systemId = systemId,
            goal = system.goal,
            duration = system.duration?.toString() ?: "",
            habits = habits,
            isLoading = false
        )
    }

    fun updateGoal(goal: String) {
        val capitalized = goal.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
        _uiState.value = _uiState.value.copy(goal = capitalized)
    }

    fun updateDuration(duration: String) {
        _uiState.value = _uiState.value.copy(duration = duration)
    }

    fun addHabit(habit: HabitItem) {
        val withId = if (habit.id == 0L) habit.copy(id = nextTempId--) else habit
        _uiState.value = _uiState.value.copy(
            habits = _uiState.value.habits + withId
        )
    }

    fun removeHabit(index: Int) {
        _uiState.value = _uiState.value.copy(
            habits = _uiState.value.habits.filterIndexed { i, _ -> i != index }
        )
    }

    fun updateHabit(index: Int, habit: HabitItem) {
        val list = _uiState.value.habits.toMutableList()
        if (index in list.indices) {
            list[index] = habit
            _uiState.value = _uiState.value.copy(habits = list)
        }
    }

    fun reorderHabits(fromIndex: Int, toIndex: Int) {
        if (fromIndex == toIndex) return
        val list = _uiState.value.habits.toMutableList()
        if (fromIndex !in list.indices || toIndex !in list.indices) return
        val item = list.removeAt(fromIndex)
        list.add(toIndex, item)
        _uiState.value = _uiState.value.copy(habits = list)
    }

    fun setNavigateBackHandled() {
        _uiState.value = _uiState.value.copy(navigateBack = false)
    }

    fun deleteGoal() {
        viewModelScope.launch {
            habitDao.deleteBySystemId(systemId)
            systemDao.deleteById(systemId)
            _uiState.value = _uiState.value.copy(navigateBack = true)
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.habits.isEmpty()) return
        val goal = state.goal.trim()
        if (goal.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            withContext(Dispatchers.IO) {
                // Re-assert suppression during save so any quick transitions don't cause streak flips.
                val today = cachedEditTodayEpochDay.takeIf { it >= 0 } ?: EffectiveDate.todayEpoch()
                val dayCompleteForSuppression = if (cachedEditTodayEpochDay == today) {
                    cachedEditTodayComplete
                } else {
                    // init() might not have run / finished yet; compute right now while habits are still unchanged.
                    streakManager.isDayComplete(systemId, today)
                }
                userPreferences.setStreakSuppressForSystem(systemId, today, dayCompleteForSuppression)

                val system = systemDao.getSystemById(systemId) ?: return@withContext
                val updated = system.copy(
                    name = goal,
                    goal = goal,
                    duration = state.duration.trim().toIntOrNull(),
                    pausedFromEpochDay = null,
                    pausedToEpochDay = null
                )
                systemDao.updateSystem(updated)
                habitDao.deleteBySystemId(systemId)
                val habits = state.habits.map { item ->
                    HabitEntity(
                        systemId = systemId,
                        title = item.title.trim(),
                        frequencyType = item.frequencyType,
                        targetCount = item.targetCount.coerceIn(1, 7),
                        trackTimeEnabled = false
                    )
                }
                habitDao.insertHabits(habits)
            }
            _uiState.value = _uiState.value.copy(isSaving = false, navigateBack = true)
        }
    }
}

class EditSystemViewModelFactory(
    private val systemId: Long,
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val userPreferences: UserPreferences,
    private val streakManager: StreakManager
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != EditSystemViewModel::class.java) throw IllegalArgumentException("Unknown ViewModel")
        return EditSystemViewModel(systemId, systemDao, habitDao, userPreferences, streakManager) as T
    }
}
