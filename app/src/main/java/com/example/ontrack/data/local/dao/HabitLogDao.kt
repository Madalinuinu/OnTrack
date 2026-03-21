package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.ontrack.data.local.entity.HabitLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitLogDao {

    @Query("SELECT * FROM habit_logs WHERE date BETWEEN :startDate AND :endDate ORDER BY date, habitId")
    fun getHabitLogsForDateRange(startDate: Long, endDate: Long): Flow<List<HabitLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: HabitLogEntity)

    @Query(
        "UPDATE habit_logs SET isCompleted = :completed, durationMinutes = :durationMinutes, isOngoing = :ongoing " +
            "WHERE habitId = :habitId AND date = :date"
    )
    suspend fun updateCompletionState(
        habitId: Long,
        date: Long,
        completed: Boolean,
        durationMinutes: Int?,
        ongoing: Boolean
    )

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
            updateCompletionState(
                habitId,
                date,
                newCompleted,
                existing.durationMinutes,
                ongoing = false
            )
        } else {
            insert(HabitLogEntity(habitId = habitId, date = date, isCompleted = true, isOngoing = false))
        }
    }

    /** Mark habit completed for date with optional duration (e.g. from timer). */
    suspend fun completeWithDuration(habitId: Long, date: Long, durationMinutes: Int?) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            updateCompletionState(habitId, date, true, durationMinutes, ongoing = false)
        } else {
            insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    isCompleted = true,
                    isOngoing = false,
                    durationMinutes = durationMinutes
                )
            )
        }
    }

    /** First tap on Today: not completed, ongoing flag set. */
    suspend fun markOngoing(habitId: Long, date: Long) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            if (existing.isCompleted) return
            updateCompletionState(
                habitId,
                date,
                completed = false,
                durationMinutes = existing.durationMinutes,
                ongoing = true
            )
        } else {
            insert(
                HabitLogEntity(
                    habitId = habitId,
                    date = date,
                    isCompleted = false,
                    isOngoing = true
                )
            )
        }
    }

    /** Second tap, notification action, etc.: mark done and clear ongoing. */
    suspend fun completeOngoingAsDone(habitId: Long, date: Long) {
        val existing = getLog(habitId, date) ?: return
        if (existing.isCompleted) return
        updateCompletionState(
            habitId,
            date,
            completed = true,
            durationMinutes = existing.durationMinutes,
            ongoing = false
        )
    }
}
