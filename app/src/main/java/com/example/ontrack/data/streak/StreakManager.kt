package com.example.ontrack.data.streak

import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.util.EffectiveDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Per-system streak: streak starts at 1 when you complete a day.
 * - Up to 2 consecutive missed days = freeze (ice 1, 2). On the 3rd consecutive missed day, streak resets.
 * - Or 5 incomplete days in the last 30 days (rolling): streak resets.
 * - Vacation days are excluded from both counts. When streak resets, display is 1 with ice.
 */
class StreakManager(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val userPreferences: UserPreferences
) {

    fun currentStreakFlow(systemId: Long): Flow<Int> =
        systemDao.getSystemByIdFlow(systemId).map { it?.currentStreak ?: 0 }

    /**
     * True if the system counts as "day complete" for streak:
     * - All DAILY habits completed that day.
     * - Each SPECIFIC_DAYS (x/week): if it's no longer possible to reach target by Sunday,
     *   then this day must be completed for that habit (otherwise the day is "frozen").
     * - WEEKLY: at least one completion in the current week by this day.
     */
    suspend fun isDayComplete(systemId: Long, epochDay: Long): Boolean {
        val habits = habitDao.getHabitsForSystem(systemId).first()
        if (habits.isEmpty()) return true

        val date = LocalDate.ofEpochDay(epochDay)
        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val weekStartEpoch = epochDay - (dayOfWeek - 1)
        val weekEndEpoch = weekStartEpoch + 6
        // Full week (Mon–Sun) so WEEKLY: "done once this week" counts for every day in that week
        val weekLogs = habitLogDao.getHabitLogsForDateRange(weekStartEpoch, weekEndEpoch).first()
        val completedCountByHabit = weekLogs
            .filter { it.isCompleted }
            .groupBy { it.habitId }
            .mapValues { (_, logs) -> logs.distinctBy { it.date }.size }

        for (habit in habits) {
            when (habit.frequencyType) {
                FrequencyType.DAILY -> {
                    val log = habitLogDao.getLog(habit.id, epochDay) ?: return false
                    if (!log.isCompleted) return false
                }
                FrequencyType.WEEKLY -> {
                    val count = completedCountByHabit[habit.id] ?: 0
                    if (count < 1) return false
                }
                FrequencyType.SPECIFIC_DAYS -> {
                    val completionsSoFar = completedCountByHabit[habit.id] ?: 0
                    val need = habit.targetCount - completionsSoFar
                    val daysLeftInWeek = (weekEndEpoch - epochDay + 1).toInt()
                    if (need > daysLeftInWeek) {
                        // Cannot reach target by Sunday → this day must be completed or it counts as freeze
                        val log = habitLogDao.getLog(habit.id, epochDay) ?: return false
                        if (!log.isCompleted) return false
                    }
                }
            }
        }
        return true
    }

    /**
     * Recomputes streak for the given system and persists to Room.
     * When vacation mode is on (today >= vacation from day), streak is not updated (frozen).
     * Vacation days (persisted) are excluded from missed-day counts so streak continues after vacation.
     */
    suspend fun refreshStreak(systemId: Long) {
        val system = systemDao.getSystemById(systemId) ?: return
        val today = EffectiveDate.todayEpoch()
        val vacationOn = userPreferences.vacationModeEnabled.first()
        val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
        if (vacationOn && vacationFrom >= 0 && today >= vacationFrom) return
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val yesterday = today - 1
        val streak = system.currentStreak
        val lastDate = system.lastStreakDate
        val todayComplete = isDayComplete(systemId, today)

        if (todayComplete) {
            val newStreak = when {
                lastDate == today -> streak
                lastDate == yesterday -> streak + 1
                lastDate < 0 -> 1
                lastDate < yesterday -> {
                    val effectiveMissedDays = (lastDate + 1..yesterday).count { it !in vacationDays }
                    if (effectiveMissedDays <= 2) streak + 1 else 1
                }
                else -> 1
            }
            systemDao.updateStreak(systemId, newStreak, today)
            return
        }

        val effectiveConsecutiveMissed = when {
            lastDate < 0 -> 1L
            else -> (lastDate + 1..today).count { it !in vacationDays }.toLong()
        }
        // Incomplete (freeze) days in last 30 days (rolling); vacation excluded
        val last30Start = today - 29
        var incompleteInLast30 = 0
        for (d in last30Start..today) {
            if (d !in vacationDays && !isDayComplete(systemId, d)) incompleteInLast30++
        }
        val shouldReset = effectiveConsecutiveMissed >= 3 || incompleteInLast30 >= 5
        if (shouldReset) {
            systemDao.resetStreak(systemId)
            // Do not clear habit logs: keep completion history; only reset streak counter. UI shows 1 with ice.
        }
    }

    /**
     * Recomputes global streak (all goals) and persists to UserPreferences.
     * Same rules as per-goal: 3 consecutive incomplete or 5 incomplete days in last 30 days = reset. Vacation excluded.
     * When vacation mode is on (today >= vacation from day), global streak is not updated.
     */
    suspend fun refreshGlobalStreak(systemIds: List<Long>) {
        if (systemIds.isEmpty()) return
        val today = EffectiveDate.todayEpoch()
        val vacationOn = userPreferences.vacationModeEnabled.first()
        val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
        if (vacationOn && vacationFrom >= 0 && today >= vacationFrom) return
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val yesterday = today - 1
        val streak = userPreferences.currentStreak.first()
        val lastDate = userPreferences.lastStreakDate.first()
        val todayComplete = systemIds.all { isDayComplete(it, today) }

        if (todayComplete) {
            val newStreak = when {
                lastDate == today -> streak
                lastDate == yesterday -> streak + 1
                lastDate < 0 -> 1
                lastDate < yesterday -> {
                    val effectiveMissedDays = (lastDate + 1..yesterday).count { it !in vacationDays }
                    if (effectiveMissedDays <= 2) streak + 1 else 1
                }
                else -> 1
            }
            userPreferences.setStreak(newStreak, today)
            return
        }

        val effectiveConsecutiveMissed = when {
            lastDate < 0 -> 1L
            else -> (lastDate + 1..today).count { it !in vacationDays }.toLong()
        }
        // Incomplete days (any goal not complete) in last 30 days; vacation excluded
        val last30Start = today - 29
        var incompleteInLast30 = 0
        for (d in last30Start..today) {
            if (d !in vacationDays && !systemIds.all { isDayComplete(it, d) }) incompleteInLast30++
        }
        val shouldReset = effectiveConsecutiveMissed >= 3 || incompleteInLast30 >= 5
        if (shouldReset) {
            userPreferences.resetGlobalStreak()
        }
    }

    fun globalStreakFlow(): Flow<Int> = userPreferences.currentStreak

    /**
     * Number of consecutive missed days (freeze/ice), 0..2. 0 if today is complete, on vacation, or no previous streak.
     * Vacation days are excluded from the count.
     */
    suspend fun getFreezeCount(systemId: Long): Int {
        val system = systemDao.getSystemById(systemId) ?: return 0
        val today = EffectiveDate.todayEpoch()
        val vacationOn = userPreferences.vacationModeEnabled.first()
        val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
        if (vacationOn && vacationFrom >= 0 && today >= vacationFrom) return 0
        if (isDayComplete(systemId, today)) return 0
        val last = system.lastStreakDate
        if (last < 0) return 0
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val effectiveGap = (last + 1..today).count { it !in vacationDays }
        return minOf(2, effectiveGap)
    }
}
