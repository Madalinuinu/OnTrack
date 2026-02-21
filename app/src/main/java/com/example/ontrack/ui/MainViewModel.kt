package com.example.ontrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val userPreferences: UserPreferences,
    initialPageFromIntent: Int = 0
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

    val trackTimeEnabled: StateFlow<Boolean> = userPreferences.trackTimeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val skipOnboardingEnabled: StateFlow<Boolean> = userPreferences.skipOnboardingEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    private val _initialPageToUse = MutableStateFlow(initialPageFromIntent)
    val initialPageToUse: StateFlow<Int> = _initialPageToUse.asStateFlow()

    fun setTrackTimeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setTrackTimeEnabled(enabled)
        }
    }

    fun setSkipOnboardingEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSkipOnboardingEnabled(enabled)
        }
    }

    fun consumeInitialPage() {
        _initialPageToUse.value = 0
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            userPreferences.setFirstLaunchComplete(name)
        }
    }
}
