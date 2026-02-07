package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ontrack.data.local.entity.HabitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert
    suspend fun insertHabits(habits: List<HabitEntity>)

    @Query("SELECT * FROM habits WHERE systemId = :systemId ORDER BY id")
    fun getHabitsForSystem(systemId: Long): Flow<List<HabitEntity>>
}
