package com.example.ontrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val userPreferences: UserPreferences
) : ViewModel() {

    /** null = preferences not loaded yet, true = show onboarding, false = show app */
    val isFirstLaunch: StateFlow<Boolean?> = userPreferences.isFirstLaunch
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val userName: StateFlow<String> = userPreferences.userName
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ""
        )

    val darkMode: StateFlow<Boolean> = userPreferences.darkMode
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    fun toggleDarkMode() {
        viewModelScope.launch {
            userPreferences.setDarkMode(!darkMode.value)
        }
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            userPreferences.setFirstLaunchComplete(name)
        }
    }
}
