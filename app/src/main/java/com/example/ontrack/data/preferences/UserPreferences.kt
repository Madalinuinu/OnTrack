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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

private object Keys {
    val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    val USER_NAME = stringPreferencesKey("user_name")
    val TRACK_TIME_ENABLED = booleanPreferencesKey("track_time_enabled")
    val SKIP_ONBOARDING_ENABLED = booleanPreferencesKey("skip_onboarding_enabled")
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    val DARK_MODE = booleanPreferencesKey("dark_mode")
    /** Global streak (all goals): current count. */
    val CURRENT_STREAK = intPreferencesKey("current_streak")
    /** Global streak: last day that was fully complete (epoch day). */
    val LAST_STREAK_DATE = longPreferencesKey("last_streak_date")
    /** Global streak: month key for 5-freeze-days-per-month rule (year*12+month). */
    val GLOBAL_FREEZE_MONTH_KEY = intPreferencesKey("global_freeze_month_key")
    /** Global streak: freeze days used in that month. */
    val GLOBAL_FREEZE_DAYS_USED_THIS_MONTH = intPreferencesKey("global_freeze_days_used_this_month")
    /** System IDs (as strings) for which the "goal time has passed" dialog was already shown. */
    val EXPIRED_GOAL_DIALOG_SHOWN_IDS = stringSetPreferencesKey("expired_goal_dialog_shown_ids")
    /** Comma-separated habit IDs for Today page task order (user-defined). */
    val TODAY_TASK_ORDER_IDS = stringPreferencesKey("today_task_order_ids")
    /** Global vacation mode: when true, streak is frozen and days show orange in Activity. */
    val VACATION_MODE_ENABLED = booleanPreferencesKey("vacation_mode_enabled")
    /** Epoch day when vacation mode was turned on; -1 when off. */
    val VACATION_MODE_FROM_EPOCH_DAY = longPreferencesKey("vacation_mode_from_epoch_day")
    /** Epoch days (as strings) that were vacation days and stay orange after vacation is turned off. */
    val PERSISTED_VACATION_EPOCH_DAYS = stringSetPreferencesKey("persisted_vacation_epoch_days")
    /** Sleep reminder: bedtime in minutes since midnight (0..1439). -1 = not set. */
    val SLEEP_BEDTIME_MINUTES = intPreferencesKey("sleep_bedtime_minutes")
    /** Sleep reminder: wake time in minutes since midnight (0..1439). -1 = not set. */
    val SLEEP_WAKE_MINUTES = intPreferencesKey("sleep_wake_minutes")

    /**
     * When editing a goal, we suppress streak recalculation for the current day.
     * Value is an epochDay; when it equals today's epochDay, StreakManager won't update streak counters.
     */
    val STREAK_SUPPRESS_EPOCH_DAY = longPreferencesKey("streak_suppress_epoch_day")

    /** System ID whose day-complete result we cache while editing. */
    val STREAK_SUPPRESS_SYSTEM_ID = longPreferencesKey("streak_suppress_system_id")

    /** Cached isDayComplete(systemId, todayEpoch) value used while editing. */
    val STREAK_SUPPRESS_SYSTEM_TODAY_COMPLETE = booleanPreferencesKey("streak_suppress_system_today_complete")

    /** Your Stats: last picked system (goal) id; -1 = use first goal until user picks. */
    val STATS_SELECTED_GOAL_SYSTEM_ID = longPreferencesKey("stats_selected_goal_system_id")
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

    val notificationsEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.NOTIFICATIONS_ENABLED] ?: true
    }

    val soundEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.SOUND_ENABLED] ?: true
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

    val todayTaskOrderIds: Flow<List<Long>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.TODAY_TASK_ORDER_IDS] ?: return@map emptyList()
        raw.split(",").mapNotNull { it.trim().toLongOrNull() }
    }

    val vacationModeEnabled: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.VACATION_MODE_ENABLED] ?: false
    }

    val vacationModeFromEpochDay: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.VACATION_MODE_FROM_EPOCH_DAY] ?: -1L
    }

    val persistedVacationEpochDays: Flow<Set<Long>> = dataStore.data.map { prefs ->
        (prefs[Keys.PERSISTED_VACATION_EPOCH_DAYS] ?: emptySet())
            .mapNotNull { it.toLongOrNull() }
            .toSet()
    }

    val sleepBedtimeMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_BEDTIME_MINUTES] ?: -1
    }

    val sleepWakeMinutes: Flow<Int> = dataStore.data.map { prefs ->
        prefs[Keys.SLEEP_WAKE_MINUTES] ?: -1
    }

    val streakSuppressEpochDay: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.STREAK_SUPPRESS_EPOCH_DAY] ?: -1L
    }

    val streakSuppressSystemId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.STREAK_SUPPRESS_SYSTEM_ID] ?: -1L
    }

    val streakSuppressSystemTodayComplete: Flow<Boolean> = dataStore.data.map { prefs ->
        prefs[Keys.STREAK_SUPPRESS_SYSTEM_TODAY_COMPLETE] ?: false
    }

    val statsSelectedGoalSystemId: Flow<Long> = dataStore.data.map { prefs ->
        prefs[Keys.STATS_SELECTED_GOAL_SYSTEM_ID] ?: -1L
    }

    suspend fun setSleepTimes(bedtimeMinutes: Int, wakeMinutes: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.SLEEP_BEDTIME_MINUTES] = bedtimeMinutes.coerceIn(0, 1439)
            prefs[Keys.SLEEP_WAKE_MINUTES] = wakeMinutes.coerceIn(0, 1439)
        }
    }

    /** Suppress streak updates while editing (only for the given epochDay). */
    suspend fun setStreakSuppressEpochDay(epochDay: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.STREAK_SUPPRESS_EPOCH_DAY] = epochDay
        }
    }

    /** Cache day-complete result while editing a system for a specific epoch day. */
    suspend fun setStreakSuppressForSystem(systemId: Long, epochDay: Long, dayComplete: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.STREAK_SUPPRESS_EPOCH_DAY] = epochDay
            prefs[Keys.STREAK_SUPPRESS_SYSTEM_ID] = systemId
            prefs[Keys.STREAK_SUPPRESS_SYSTEM_TODAY_COMPLETE] = dayComplete
        }
    }

    /** Clear streak suppression (used when user completes tasks after editing). */
    suspend fun clearStreakSuppress() {
        dataStore.edit { prefs ->
            prefs[Keys.STREAK_SUPPRESS_EPOCH_DAY] = -1L
            prefs[Keys.STREAK_SUPPRESS_SYSTEM_ID] = -1L
            prefs[Keys.STREAK_SUPPRESS_SYSTEM_TODAY_COMPLETE] = false
        }
    }

    suspend fun getSleepTimes(): Pair<Int, Int> {
        val prefs = dataStore.data.first()
        return Pair(
            prefs[Keys.SLEEP_BEDTIME_MINUTES] ?: -1,
            prefs[Keys.SLEEP_WAKE_MINUTES] ?: -1
        )
    }

    suspend fun setVacationModeEnabled(enabled: Boolean, fromEpochDay: Long = -1L) {
        dataStore.edit { prefs ->
            prefs[Keys.VACATION_MODE_ENABLED] = enabled
            prefs[Keys.VACATION_MODE_FROM_EPOCH_DAY] = if (enabled) fromEpochDay else -1L
        }
    }

    /** Add the range [fromEpochDay, toEpochDay] to persisted vacation days (e.g. when user turns off vacation). */
    suspend fun addPersistedVacationDaysRange(fromEpochDay: Long, toEpochDay: Long) {
        if (fromEpochDay < 0 || toEpochDay < fromEpochDay) return
        dataStore.edit { prefs ->
            val current = prefs[Keys.PERSISTED_VACATION_EPOCH_DAYS] ?: emptySet()
            val toAdd = (fromEpochDay..toEpochDay).map { it.toString() }.toSet()
            prefs[Keys.PERSISTED_VACATION_EPOCH_DAYS] = current + toAdd
        }
    }

    suspend fun setTodayTaskOrderIds(habitIds: List<Long>) {
        dataStore.edit { prefs ->
            prefs[Keys.TODAY_TASK_ORDER_IDS] = habitIds.joinToString(",")
        }
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

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSoundEnabled(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.SOUND_ENABLED] = enabled
        }
    }

    suspend fun setDarkMode(enabled: Boolean) {
        dataStore.edit { prefs ->
            prefs[Keys.DARK_MODE] = enabled
        }
    }

    /** Set global streak (all goals complete). */
    suspend fun setStreak(streak: Int, lastDate: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT_STREAK] = streak
            prefs[Keys.LAST_STREAK_DATE] = lastDate
        }
    }

    /** Reset global streak and clear freeze-month state (e.g. after 3 consecutive or 5/month). */
    suspend fun resetGlobalStreak() {
        dataStore.edit { prefs ->
            prefs[Keys.CURRENT_STREAK] = 0
            prefs[Keys.LAST_STREAK_DATE] = -1L
            prefs[Keys.GLOBAL_FREEZE_MONTH_KEY] = 0
            prefs[Keys.GLOBAL_FREEZE_DAYS_USED_THIS_MONTH] = 0
        }
    }

    suspend fun setGlobalFreezeMonth(monthKey: Int, daysUsed: Int) {
        dataStore.edit { prefs ->
            prefs[Keys.GLOBAL_FREEZE_MONTH_KEY] = monthKey
            prefs[Keys.GLOBAL_FREEZE_DAYS_USED_THIS_MONTH] = daysUsed
        }
    }

    suspend fun setStatsSelectedGoalSystemId(systemId: Long) {
        dataStore.edit { prefs ->
            prefs[Keys.STATS_SELECTED_GOAL_SYSTEM_ID] = systemId
        }
    }

    suspend fun getGlobalFreezeMonth(): Pair<Int, Int> {
        val prefs = dataStore.data.first()
        return Pair(
            prefs[Keys.GLOBAL_FREEZE_MONTH_KEY] ?: 0,
            prefs[Keys.GLOBAL_FREEZE_DAYS_USED_THIS_MONTH] ?: 0
        )
    }
}
