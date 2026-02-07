package com.example.ontrack.data.streak

import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.preferences.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.time.LocalDate

/**
 * Manages streak calculation and persistence.
 * A "Streak Day" counts only when ALL Daily habits (across all systems) are completed for that day.
 */
class StreakManager(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences
) {

    val currentStreak: Flow<Int> = userPreferences.currentStreak

    /**
     * Returns true if the given day (epoch day) counts as a streak day:
     * every DAILY habit in every system has a completed log for that day.
     * If there are no daily habits, returns true (vacuous; doesn't break streak).
     */
    suspend fun isDayComplete(epochDay: Long): Boolean {
        val systems = systemDao.getAllSystems().first()
        val dailyHabits = mutableListOf<HabitEntity>()
        for (system in systems) {
            val habits = habitDao.getHabitsForSystem(system.id).first()
            dailyHabits.addAll(habits.filter { it.frequencyType == FrequencyType.DAILY })
        }
        if (dailyHabits.isEmpty()) return true
        for (habit in dailyHabits) {
            val log = habitLogDao.getLog(habit.id, epochDay) ?: return false
            if (!log.isCompleted) return false
        }
        return true
    }

    /**
     * Recomputes streak and persists to DataStore. Call on app launch / HomeScreen and after completing habits.
     * - If yesterday was missed (lastStreakDate < yesterday): reset streak to 0.
     * - If today is complete and not yet counted: increment streak (or set to 1 if was reset), set lastStreakDate = today.
     */
    suspend fun refreshStreak() {
        val today = LocalDate.now().toEpochDay()
        val yesterday = today - 1
        val streak = userPreferences.currentStreak.first()
        val lastDate = userPreferences.lastStreakDate.first()

        // Missed yesterday: reset
        if (lastDate >= 0 && lastDate < yesterday) {
            userPreferences.setStreak(0, -1L)
            return
        }

        // Today just completed: update streak
        if (isDayComplete(today)) {
            val newStreak = when {
                lastDate == today -> streak
                lastDate == yesterday -> streak + 1
                else -> 1
            }
            userPreferences.setStreak(newStreak, today)
        }
    }
}
