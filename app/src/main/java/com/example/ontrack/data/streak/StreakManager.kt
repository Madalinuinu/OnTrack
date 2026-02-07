package com.example.ontrack.data.streak

import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyType
import com.example.ontrack.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.LocalDate

/**
 * Per-system streak: streak starts at 1 when you complete a day.
 * - Up to 3 consecutive missed days = freeze (ice 1, 2, 3). On the 4th consecutive missed day, streak resets.
 * - Max 5 freeze days per calendar month: if you use more than 5 freeze days in one month, streak resets.
 * Completing any day in the freeze window clears all ice and counts as streak (at least 1).
 */
class StreakManager(
    private val systemDao: SystemDao,
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao
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
        val weekLogs = habitLogDao.getHabitLogsForDateRange(weekStartEpoch, epochDay).first()
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
     */
    suspend fun refreshStreak(systemId: Long) {
        val system = systemDao.getSystemById(systemId) ?: return
        val today = LocalDate.now().toEpochDay()
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
                    val missedDays = (yesterday - lastDate).toInt()
                    if (missedDays <= 3) streak else 1
                }
                else -> 1
            }
            systemDao.updateStreak(systemId, newStreak, today)
            return
        }

        val consecutiveMissed = when {
            lastDate < 0 -> 1L
            else -> (today - lastDate)
        }
        val todayLocal = LocalDate.ofEpochDay(today)
        val currentMonthKey = todayLocal.year * 12 + todayLocal.monthValue

        val shouldReset = when {
            consecutiveMissed >= 4 -> true
            else -> {
                val existingFreezeThisMonth = if (system.freezeMonthKey == currentMonthKey) system.freezeDaysUsedThisMonth else 0
                var freezeDaysInCurrentRunThisMonth = 0
                var d = lastDate + 1
                while (d <= today) {
                    val ld = LocalDate.ofEpochDay(d)
                    if (ld.year == todayLocal.year && ld.monthValue == todayLocal.monthValue) freezeDaysInCurrentRunThisMonth++
                    d++
                }
                val totalFreezeThisMonth = existingFreezeThisMonth + freezeDaysInCurrentRunThisMonth
                if (totalFreezeThisMonth > 5) true
                else {
                    systemDao.updateFreezeMonth(systemId, currentMonthKey, totalFreezeThisMonth)
                    false
                }
            }
        }
        if (shouldReset) {
            systemDao.resetStreak(systemId)
            // Do not clear habit logs: keep completion history; only reset streak counter.
        }
    }

    /**
     * Number of consecutive missed days (freeze/ice), 0..3. 0 if today is complete or no previous streak.
     */
    suspend fun getFreezeCount(systemId: Long): Int {
        val system = systemDao.getSystemById(systemId) ?: return 0
        val today = LocalDate.now().toEpochDay()
        if (isDayComplete(systemId, today)) return 0
        val last = system.lastStreakDate
        if (last < 0) return 0
        val gap = (today - last).toInt()
        return minOf(3, gap)
    }
}
