package com.example.ontrack.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private object Keys {
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val USER_NAME = stringPreferencesKey("user_name")
    val CURRENT_STREAK = intPreferencesKey("current_streak")
    val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
}

class UserPreferences(context: Context) {

    private val dataStore = context.applicationContext.dataStore

    val isFirstLaunch: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.IS_FIRST_LAUNCH] ?: true
    }

    val userName: Flow<String> = dataStore.data.map { prefs ->
        prefs[Keys.USER_NAME] ?: ""
    }

    val currentStreak: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.CURRENT_STREAK] ?: 0
    }

    val lastStreakDate: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.LAST_STREAK_DATE] ?: -1L
    }

    suspend fun setFirstLaunchComplete(name: String) {
        dataStore.edit { prefs ->
            prefs[Keys.IS_FIRST_LAUNCH] = false
            prefs[Keys.USER_NAME] = name
        }
    }

    suspend fun setStreak(streak: Int, lastDate: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT_STREAK] = streak
            prefs[Keys.LAST_STREAK_DATE] = lastDate
        }
    }
}
