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

    @Query("UPDATE habit_logs SET isCompleted = :completed WHERE habitId = :habitId AND date = :date")
    suspend fun updateCompletion(habitId: Long, date: Long, completed: Boolean)

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND date = :date LIMIT 1")
    suspend fun getLog(habitId: Long, date: Long): HabitLogEntity?

    suspend fun toggleHabitCompletion(habitId: Long, date: Long) {
        val existing = getLog(habitId, date)
        if (existing != null) {
            updateCompletion(habitId, date, !existing.isCompleted)
        } else {
            insert(HabitLogEntity(habitId = habitId, date = date, isCompleted = true))
        }
    }
}