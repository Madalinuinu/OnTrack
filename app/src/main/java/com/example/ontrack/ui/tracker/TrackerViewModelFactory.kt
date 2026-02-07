package com.example.ontrack.ui.tracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ontrack.data.local.AppDatabase
import com.example.ontrack.data.streak.StreakManager

class TrackerViewModelFactory(
    private val database: AppDatabase,
    private val streakManager: StreakManager,
    private val systemId: Long
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TrackerViewModel::class.java)) {
            return TrackerViewModel(
                systemDao = database.systemDao(),
                habitDao = database.habitDao(),
                habitLogDao = database.habitLogDao(),
                streakManager = streakManager,
                systemId = systemId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
