package com.example.ontrack.ui.createsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HabitItem(
    val id: Long = 0L,
    val title: String,
    val frequencyType: FrequencyType,
    val targetCount: Int = 1,
    val trackTimeEnabled: Boolean = false
)

data class CreateSystemUiState(
    val systemGoal: String = "",
    val duration: String = "",
    val habits: List<HabitItem> = emptyList(),
    val isSaving: Boolean = false,
    val navigateBack: Boolean = false
)

class CreateSystemViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val userPreferences: UserPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSystemUiState())
    val uiState: StateFlow<CreateSystemUiState> = _uiState.asStateFlow()

    /** Stable negative ids for new habits so reorderable list keys don't conflict. */
    private var nextTempId = -1L

    fun updateGoal(goal: String) {
        val capitalized = goal.replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }
        _uiState.value = _uiState.value.copy(systemGoal = capitalized)
    }

    fun updateDuration(duration: String) {
        val digitsOnly = duration.filter { it.isDigit() }
        _uiState.value = _uiState.value.copy(duration = digitsOnly)
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

    fun createSystem() {
        val state = _uiState.value
        val goal = state.systemGoal.trim()
        if (goal.isBlank()) return

        if (state.habits.isEmpty()) return
        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            withContext(Dispatchers.IO) {
                val durationInt = state.duration.trim().toIntOrNull()
                val sortOrder = systemDao.nextSortOrder()
                val system = SystemEntity(
                    name = goal,
                    goal = goal,
                    duration = durationInt,
                    startDate = System.currentTimeMillis(),
                    sortOrder = sortOrder,
                    isTestData = false
                )
                val systemId = systemDao.insertSystem(system)
                val habits = state.habits.map { item ->
                    HabitEntity(
                        systemId = systemId,
                        title = item.title.trim(),
                        frequencyType = item.frequencyType,
                        targetCount = item.targetCount.coerceIn(1, 7),
                        trackTimeEnabled = item.trackTimeEnabled
                    )
                }
                habitDao.insertHabits(habits)
            }
            _uiState.value = _uiState.value.copy(isSaving = false, navigateBack = true)
        }
    }
}
