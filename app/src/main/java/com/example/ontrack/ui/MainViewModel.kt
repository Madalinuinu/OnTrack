package com.example.ontrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
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

    val notificationsEnabled: StateFlow<Boolean> = userPreferences.notificationsEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val soundEnabled: StateFlow<Boolean> = userPreferences.soundEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = true
        )

    val vacationModeEnabled: StateFlow<Boolean> = userPreferences.vacationModeEnabled
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = false
        )

    val vacationModeFromEpochDay: StateFlow<Long> = userPreferences.vacationModeFromEpochDay
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = -1L
        )

    val persistedVacationEpochDays: StateFlow<Set<Long>> = userPreferences.persistedVacationEpochDays
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptySet()
        )

    val sleepBedtimeMinutes: StateFlow<Int> = userPreferences.sleepBedtimeMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = -1
        )

    val sleepWakeMinutes: StateFlow<Int> = userPreferences.sleepWakeMinutes
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = -1
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

    fun setNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setNotificationsEnabled(enabled)
        }
    }

    fun setSoundEnabled(enabled: Boolean) {
        viewModelScope.launch {
            userPreferences.setSoundEnabled(enabled)
        }
    }

    fun setVacationModeEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val todayEpoch = com.example.ontrack.util.EffectiveDate.todayEpoch()
            if (!enabled) {
                val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
                if (vacationFrom >= 0) {
                    // Include today so the day we turn off is still vacation (orange, no tasks); resume next day
                    if (todayEpoch >= vacationFrom) {
                        userPreferences.addPersistedVacationDaysRange(vacationFrom, todayEpoch)
                    }
                }
            }
            val fromEpochDay = when {
                enabled -> todayEpoch + 1
                else -> -1L
            }
            userPreferences.setVacationModeEnabled(enabled, fromEpochDay)
        }
    }

    fun consumeInitialPage() {
        _initialPageToUse.value = 0
    }

    /** Call before navigating to home so the Today tab is shown (e.g. from Test screen). */
    fun setInitialPageToToday() {
        _initialPageToUse.value = 1
    }

    fun completeOnboarding(name: String) {
        viewModelScope.launch {
            userPreferences.setFirstLaunchComplete(name)
        }
    }

    fun setSleepTimes(bedtimeMinutes: Int, wakeMinutes: Int) {
        viewModelScope.launch {
            userPreferences.setSleepTimes(bedtimeMinutes, wakeMinutes)
        }
    }
}
