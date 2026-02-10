package com.example.ontrack.ui.editsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.ui.createsystem.HabitItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

data class EditSystemUiState(
    val systemId: Long = 0L,
    val goal: String = "",
    val duration: String = "",
    val habits: List<HabitItem> = emptyList(),
    val pausedFromDate: String = "",
    val pausedToDate: String = "",
    val isSaving: Boolean = false,
    val isLoading: Boolean = true,
    val navigateBack: Boolean = false
)

class EditSystemViewModel(
    private val systemId: Long,
    private val systemDao: SystemDao,
    private val habitDao: HabitDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditSystemUiState(systemId = systemId))
    val uiState: StateFlow<EditSystemUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch { load() }
    }

    private suspend fun load() {
        val system = systemDao.getSystemById(systemId) ?: run {
            _uiState.value = _uiState.value.copy(isLoading = false, navigateBack = true)
            return
        }
        val habits = habitDao.getHabitsForSystem(systemId).first().map { h ->
            HabitItem(
                title = h.title,
                frequencyType = h.frequencyType,
                targetCount = h.targetCount
            )
        }
        val formatter = DateTimeFormatter.ISO_LOCAL_DATE
        val fromStr = system.pausedFromEpochDay?.let { LocalDate.ofEpochDay(it).format(formatter) } ?: ""
        val toStr = system.pausedToEpochDay?.let { LocalDate.ofEpochDay(it).format(formatter) } ?: ""
        _uiState.value = EditSystemUiState(
            systemId = systemId,
            goal = system.goal,
            duration = system.duration?.toString() ?: "",
            habits = habits,
            pausedFromDate = fromStr,
            pausedToDate = toStr,
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

    fun updatePausedFrom(dateStr: String) {
        _uiState.value = _uiState.value.copy(pausedFromDate = dateStr)
    }

    fun updatePausedTo(dateStr: String) {
        _uiState.value = _uiState.value.copy(pausedToDate = dateStr)
    }

    fun setPauseRange(fromDate: String, toDate: String) {
        _uiState.value = _uiState.value.copy(
            pausedFromDate = fromDate,
            pausedToDate = toDate
        )
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

    fun updateHabit(index: Int, habit: HabitItem) {
        val list = _uiState.value.habits.toMutableList()
        if (index in list.indices) {
            list[index] = habit
            _uiState.value = _uiState.value.copy(habits = list)
        }
    }

    fun setNavigateBackHandled() {
        _uiState.value = _uiState.value.copy(navigateBack = false)
    }

    private fun parseDate(s: String): Long? {
        if (s.isBlank()) return null
        return try {
            LocalDate.parse(s.trim(), DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()
        } catch (_: DateTimeParseException) {
            null
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
                val system = systemDao.getSystemById(systemId) ?: return@withContext
                val fromEpoch = parseDate(state.pausedFromDate)
                val toEpoch = parseDate(state.pausedToDate)
                val pausedFrom = if (fromEpoch != null && toEpoch != null) minOf(fromEpoch, toEpoch) else fromEpoch
                val pausedTo = if (fromEpoch != null && toEpoch != null) maxOf(fromEpoch, toEpoch) else toEpoch
                val updated = system.copy(
                    name = goal,
                    goal = goal,
                    duration = state.duration.trim().toIntOrNull(),
                    pausedFromEpochDay = pausedFrom,
                    pausedToEpochDay = pausedTo
                )
                systemDao.updateSystem(updated)
                habitDao.deleteBySystemId(systemId)
                val habits = state.habits.map { item ->
                    HabitEntity(
                        systemId = systemId,
                        title = item.title.trim(),
                        frequencyType = item.frequencyType,
                        targetCount = item.targetCount.coerceIn(1, 7)
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
    private val habitDao: HabitDao
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass != EditSystemViewModel::class.java) throw IllegalArgumentException("Unknown ViewModel")
        return EditSystemViewModel(systemId, systemDao, habitDao) as T
    }
}
