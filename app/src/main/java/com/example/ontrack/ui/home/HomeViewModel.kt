package com.example.ontrack.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.SystemEntity
import com.example.ontrack.data.streak.StreakManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val systemDao: SystemDao,
    private val streakManager: StreakManager
) : ViewModel() {

    val systems: StateFlow<List<SystemEntity>> = systemDao.getAllSystems()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

    val currentStreak: StateFlow<Int> = streakManager.currentStreak
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = 0
        )

    init {
        viewModelScope.launch { streakManager.refreshStreak() }
    }
}
