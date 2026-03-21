package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ontrack.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Insert
    suspend fun insertHabit(habit: HabitEntity): Long

    @Query("DELETE FROM habits WHERE id = :habitId")
    suspend fun deleteHabitById(habitId: Long)

    @Query("SELECT * FROM habits WHERE systemId = :systemId ORDER BY id")
    fun getHabitsForSystem(systemId: Long): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits ORDER BY systemId, id")
    fun getAllHabits(): Flow<List<HabitEntity>>

    @Query("DELETE FROM habits WHERE systemId = :systemId")
    suspend fun deleteBySystemId(systemId: Long)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Query("UPDATE habits SET lastTimerDurationSeconds = :seconds WHERE id = :habitId")
    suspend fun updateLastTimerDuration(habitId: Long, seconds: Int)
}
