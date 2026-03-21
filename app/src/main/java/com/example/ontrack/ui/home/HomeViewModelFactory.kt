package com.example.ontrack.ui.home

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ontrack.data.local.AppDatabase
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.data.streak.StreakManager

class HomeViewModelFactory(
    private val database: AppDatabase,
    private val streakManager: StreakManager,
    private val userPreferences: UserPreferences,
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                database.systemDao(),
                database.habitDao(),
                database.habitLogDao(),
                streakManager,
                userPreferences,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
