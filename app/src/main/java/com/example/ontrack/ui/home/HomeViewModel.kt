package com.example.ontrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.streak.StreakManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate

class HomeViewModel(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val streakManager: StreakManager
) : ViewModel() {

    private val _selectedSystemId = MutableStateFlow<Long?>(null)
    val selectedSystemId: StateFlow<Long?> = _selectedSystemId.asStateFlow()

    val systems: StateFlow<List<SystemEntity>> = systemDao.getAllSystems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    private val _todayCompleteMap = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val todayCompleteMap: StateFlow<Map<Long, Boolean>> = _todayCompleteMap.asStateFlow()

    private val _freezeCountMap = MutableStateFlow<Map<Long, Int>>(emptyMap())
    val freezeCountMap: StateFlow<Map<Long, Int>> = _freezeCountMap.asStateFlow()

    init {
        viewModelScope.launch {
            systemDao.getAllSystems().first().forEach { system ->
                streakManager.refreshStreak(system.id)
            }
        }
        viewModelScope.launch {
            systems.collect { list ->
                val today = LocalDate.now().toEpochDay()
                _todayCompleteMap.value = list.associate { s ->
                    s.id to streakManager.isDayComplete(s.id, today)
                }
                _freezeCountMap.value = list.associate { s ->
                    s.id to streakManager.getFreezeCount(s.id)
                }
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

    /** Call when returning to Home so card colors and freeze counts are up to date. */
    fun refreshTodayComplete() {
        viewModelScope.launch {
            val list = systems.value
            val today = LocalDate.now().toEpochDay()
            _todayCompleteMap.value = list.associate { s ->
                s.id to streakManager.isDayComplete(s.id, today)
            }
            _freezeCountMap.value = list.associate { s ->
                s.id to streakManager.getFreezeCount(s.id)
            }
        }
    }
}
