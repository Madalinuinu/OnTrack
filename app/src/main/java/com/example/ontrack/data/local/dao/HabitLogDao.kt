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

    @Query("UPDATE habit_logs SET isCompleted = :completed, durationMinutes = :durationMinutes WHERE habitId = :habitId AND date = :date")
    suspend fun updateCompletion(habitId: Long, date: Long, completed: Boolean, durationMinutes: Int? = null)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: Long, date: Long): HabitLogEntity?

    @Query("DELETE FROM habit_logs")
    suspend fun clearAllLogs()

    @Query("DELETE FROM habit_logs WHERE habitId IN (:habitIds)")
    suspend fun clearLogsForHabits(habitIds: List<Long>)

    suspend fun toggleHabitCompletion(habitId: Long, date: Long) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            updateCompletion(habitId, date, !existing.isCompleted, existing.durationMinutes)
        } else {
            insert(HabitLogEntity(habitId = habitId, date = date, isCompleted = true))
        }
    }

    /** Mark habit completed for date with optional duration (e.g. from timer). */
    suspend fun completeWithDuration(habitId: Long, date: Long, durationMinutes: Int?) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            updateCompletion(habitId, date, true, durationMinutes)
        } else {
            insert(HabitLogEntity(habitId = habitId, date = date, isCompleted = true, durationMinutes = durationMinutes))
        }
    }
}