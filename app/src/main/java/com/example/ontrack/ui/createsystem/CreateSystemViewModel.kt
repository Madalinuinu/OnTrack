package com.example.ontrack.ui.createsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.SystemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HabitItem(
    val title: String,
    val frequencyType: FrequencyType,
    val targetCount: Int = 1
)

data class CreateSystemUiState(
    val systemName: String = "",
    val systemGoal: String = "",
    val duration: String = "",
    val habits: List<HabitItem> = emptyList(),
    val isSaving: Boolean = false,
    val navigateBack: Boolean = false
)

class CreateSystemViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateSystemUiState())
    val uiState: StateFlow<CreateSystemUiState> = _uiState.asStateFlow()

    fun updateName(name: String) {
        _uiState.value = _uiState.value.copy(systemName = name)
    }

    fun updateGoal(goal: String) {
        _uiState.value = _uiState.value.copy(systemGoal = goal)
    }

    fun updateDuration(duration: String) {
        _uiState.value = _uiState.value.copy(duration = duration)
    }

    fun addHabit(habit: HabitItem) {
        _uiState.value = _uiState.value.copy(
            habits = _uiState.value.habits + habit
        )
    }

    fun removeHabit(index: Int) {
        _uiState.value = _uiState.value.copy(
            habits = _uiState.value.habits.filterIndexed { i, _ -> i != index }
        )
    }

    fun setNavigateBackHandled() {
        _uiState.value = _uiState.value.copy(navigateBack = false)
    }

    fun createSystem() {
        val state = _uiState.value
        val name = state.systemName.trim()
        val goal = state.systemGoal.trim()
        if (name.isBlank() || goal.isBlank()) return

        viewModelScope.launch {
            _uiState.value = state.copy(isSaving = true)
            withContext(Dispatchers.IO) {
                val durationInt = state.duration.trim().toIntOrNull()
                val system = SystemEntity(
                    name = name,
                    goal = goal,
                    duration = durationInt,
                    startDate = System.currentTimeMillis()
                )
                val systemId = systemDao.insertSystem(system)
                val habits = state.habits.map { item ->
                    HabitEntity(
                        systemId = systemId,
                        title = item.title.trim(),
                        frequencyType = item.frequencyType,
                        targetCount = item.targetCount.coerceIn(1, 7)
                    )
                }
                if (habits.isNotEmpty()) {
                    habitDao.insertHabits(habits)
                }
            }
            _uiState.value = _uiState.value.copy(isSaving = false, navigateBack = true)
        }
    }
}
