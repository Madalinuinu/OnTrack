package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.ontrack.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date, habitId")
    fun getHabitLogsForDateRange(startDate: Long, endDate: Long): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLogEntity)

    @Update
    suspend fun update(log: HabitLogEntity)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: Long, date: Long): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE date = :date AND isOngoing = 1 AND isCompleted = 0")
    suspend fun getOngoingLogsForDate(date: Long): List<HabitLogEntity>

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM habit_logs WHERE habitId IN (:habitIds)")
    suspend fun clearLogsForHabits(habitIds: List<Long>)

    suspend fun toggleHabitCompletion(habitId: Long, date: Long) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            val newCompleted = !existing.isCompleted
            update(
                existing.copy(
                    isCompleted = newCompleted,
                    isOngoing = false,
                    ongoingStartedAtMillis = null,
                    durationMinutes = if (newCompleted) existing.durationMinutes else null,
                    sessionDurationSeconds = if (newCompleted) existing.sessionDurationSeconds else null
                )
            )
        } else {
            insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    isCompleted = true,
                    isOngoing = false
                )
            )
        }
    }

    /**
     * Mark completed with optional minute display and/or exact seconds (timer uses [durationSeconds]).
     */
    suspend fun completeWithDuration(
        habitId: Long,
        date: Long,
        durationMinutes: Int?,
        durationSeconds: Int? = null
    ) {
        val existing = getLog(habitId, date)
        val sessionSec: Int? = when {
            durationSeconds != null && durationSeconds > 0 -> durationSeconds
            durationMinutes != null && durationMinutes > 0 -> durationMinutes * 60
            else -> null
        }
        if (existing != null) {
            update(
                existing.copy(
                    isCompleted = true,
                    isOngoing = false,
                    ongoingStartedAtMillis = null,
                    durationMinutes = durationMinutes,
                    sessionDurationSeconds = sessionSec
                )
            )
        } else {
            insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    isCompleted = true,
                    isOngoing = false,
                    durationMinutes = durationMinutes,
                    sessionDurationSeconds = sessionSec
                )
            )
        }
    }

    /** First tap on Today: not completed, ongoing flag set; start timestamp for session length. */
    suspend fun markOngoing(habitId: Long, date: Long) {
        val existing = getLog(habitId, date)
        val now = System.currentTimeMillis()
        if (existing != null) {
            if (existing.isCompleted) return
            update(
                existing.copy(
                    isOngoing = true,
                    ongoingStartedAtMillis = now
                )
            )
        } else {
            insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    isCompleted = false,
                    isOngoing = true,
                    ongoingStartedAtMillis = now
                )
            )
        }
    }

    /** Second tap, notification action, etc.: mark done and store start→done seconds. */
    suspend fun completeOngoingAsDone(habitId: Long, date: Long) {
        val existing = getLog(habitId, date) ?: return
        if (existing.isCompleted) return
        val start = existing.ongoingStartedAtMillis
        val sessionSec: Int? = if (start != null) {
            ((System.currentTimeMillis() - start) / 1000L).toInt().coerceIn(0, Int.MAX_VALUE)
        } else {
            null
        }
        update(
            existing.copy(
                isCompleted = true,
                isOngoing = false,
                ongoingStartedAtMillis = null,
                sessionDurationSeconds = sessionSec?.takeIf { it > 0 }
            )
        )
    }
}
