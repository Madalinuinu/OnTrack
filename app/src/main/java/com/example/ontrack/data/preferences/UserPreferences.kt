package com.example.ontrack.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private object Keys {
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val USER_NAME = stringPreferencesKey("user_name")
    val TRACK_TIME_ENABLED = booleanPreferencesKey("track_time_enabled")
    val SKIP_ONBOARDING_ENABLED = booleanPreferencesKey("skip_onboarding_enabled")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    val CURRENT_STREAK = intPreferencesKey("current_streak")
    val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
    /** System IDs (as strings) for which the "goal time has passed" dialog was already shown. */
    val EXPIRED_GOAL_DIALOG_SHOWN_IDS = stringSetPreferencesKey("expired_goal_dialog_shown_ids")
}

class UserPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_FIRST_LAUNCH] ?: true
    }

    val userName: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: ""
    }

    val trackTimeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.TRACK_TIME_ENABLED] ?: true
    }

    val skipOnboardingEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SKIP_ONBOARDING_ENABLED] ?: false
    }

    val darkMode: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.DARK_MODE] ?: false
    }

    val currentStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_STREAK] ?: 0
    }

    val lastStreakDate: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_STREAK_DATE] ?: -1L
    }

    val expiredGoalDialogShownIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[Keys.EXPIRED_GOAL_DIALOG_SHOWN_IDS] ?: emptySet()
    }

    suspend fun addExpiredGoalDialogShown(systemId: Long) {
        dataStore.edit { prefs ->
            val current = prefs[Keys.EXPIRED_GOAL_DIALOG_SHOWN_IDS] ?: emptySet()
            prefs[Keys.EXPIRED_GOAL_DIALOG_SHOWN_IDS] = current + systemId.toString()
        }
    }

    suspend fun setFirstLaunchComplete(name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_FIRST_LAUNCH] = false
            prefs[Keys.USER_NAME] = name
        }
    }

    suspend fun setTrackTimeEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.TRACK_TIME_ENABLED] = enabled
        }
    }

    suspend fun setSkipOnboardingEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SKIP_ONBOARDING_ENABLED] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    suspend fun setStreak(streak: Int, lastDate: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT_STREAK] = streak
            prefs[Keys.LAST_STREAK_DATE] = lastDate
        }
    }
}
