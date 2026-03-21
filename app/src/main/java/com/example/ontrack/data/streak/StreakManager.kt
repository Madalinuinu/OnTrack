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
     * - WEEKLY (once per week): same as SPECIFIC_DAYS with target 1 — at start of week (0/1) the day counts as
     *   on-track; only on the last day of the week (Sunday) must it be completed if not done yet.
     * - SPECIFIC_DAYS (x/week): at start of week (e.g. 0/3) the day counts as on-track. When remaining completions
     *   needed >= days left in week (e.g. Friday 2/5 → need 3, 3 days left), at least one completion is required
     *   on that day to maintain streak.
     */
    suspend fun isDayComplete(systemId: Long, epochDay: Long): Boolean {
        val today = EffectiveDate.todayEpoch()
        if (epochDay == today) {
            val suppressEpochDay = userPreferences.streakSuppressEpochDay.first()
            if (suppressEpochDay == today) {
                val suppressSystemId = userPreferences.streakSuppressSystemId.first()
                if (suppressSystemId == systemId) {
                    return userPreferences.streakSuppressSystemTodayComplete.first()
                }
            }
        }

        val habits = habitDao.getHabitsForSystem(systemId).first()
        if (habits.isEmpty()) return true

        val date = LocalDate.ofEpochDay(epochDay)
        val dayOfWeek = date.dayOfWeek.value // 1 = Monday, 7 = Sunday
        val weekStartEpoch = epochDay - (dayOfWeek - 1)
        val weekEndEpoch = weekStartEpoch + 6
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
                    // Same logic as SPECIFIC_DAYS with target 1: on-track until last day, then must complete that day
                    val completionsSoFar = completedCountByHabit[habit.id] ?: 0
                    if (completionsSoFar >= 1) continue
                    val need = 1
                    val daysLeftInWeek = (weekEndEpoch - epochDay + 1).toInt()
                    if (need >= daysLeftInWeek) {
                        val log = habitLogDao.getLog(habit.id, epochDay) ?: return false
                        if (!log.isCompleted) return false
                    }
                }
                FrequencyType.SPECIFIC_DAYS -> {
                    val completionsSoFar = completedCountByHabit[habit.id] ?: 0
                    if (completionsSoFar >= habit.targetCount) {
                        // Already reached weekly target → day counts as complete
                        continue
                    }
                    val need = habit.targetCount - completionsSoFar
                    val daysLeftInWeek = (weekEndEpoch - epochDay + 1).toInt() // including today
                    // If we need at least as many completions as days left, we must do at least one today to stay on track
                    if (need >= daysLeftInWeek) {
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

        val suppressEpochDay = userPreferences.streakSuppressEpochDay.first()
        if (suppressEpochDay == today) return

        val vacationOn = userPreferences.vacationModeEnabled.first()
        val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
        if (vacationOn && vacationFrom >= 0 && today >= vacationFrom) return
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val yesterday = today - 1
        val streak = system.currentStreak
        val lastDate = system.lastStreakDate

        val todayComplete = isDayComplete(systemId, today)

        // Still complete today → nothing to recompute (avoids churn from refresh calls).
        if (lastDate == today && todayComplete) return

        // Streak was credited for today but user unchecked tasks: undo today's credit (same UI as before completing).
        if (lastDate == today && !todayComplete) {
            val newStreak = (streak - 1).coerceAtLeast(0)
            val newLast = if (newStreak <= 0) -1L else yesterday
            systemDao.updateStreak(systemId, newStreak, newLast)
            return
        }

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

        val suppressEpochDay = userPreferences.streakSuppressEpochDay.first()
        if (suppressEpochDay == today) return

        val vacationOn = userPreferences.vacationModeEnabled.first()
        val vacationFrom = userPreferences.vacationModeFromEpochDay.first()
        if (vacationOn && vacationFrom >= 0 && today >= vacationFrom) return
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val yesterday = today - 1
        val streak = userPreferences.currentStreak.first()
        val lastDate = userPreferences.lastStreakDate.first()
        val todayComplete = systemIds.all { isDayComplete(it, today) }

        // Global streak was credited for today but a goal was unchecked — undo today's credit.
        if (lastDate == today && !todayComplete) {
            val newStreak = (streak - 1).coerceAtLeast(0)
            val newLast = if (newStreak <= 0) -1L else yesterday
            userPreferences.setStreak(newStreak, newLast)
            return
        }

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
        val last = system.lastStreakDate
        if (last < 0) return 0
        val vacationDays = userPreferences.persistedVacationEpochDays.first()
        val effectiveGap = (last + 1..today).count { it !in vacationDays }
        return minOf(2, effectiveGap)
    }
}
