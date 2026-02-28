package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.ontrack.data.local.entity.SystemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {

    @Insert
    suspend fun insertSystem(system: SystemEntity): Long

    @Update
    suspend fun updateSystem(system: SystemEntity)

    @Query("SELECT * FROM systems ORDER BY sortOrder ASC, startDate DESC")
    fun getAllSystems(): Flow<List<SystemEntity>>

    @Query("DELETE FROM systems WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM systems WHERE id = :id LIMIT 1")
    suspend fun getSystemById(id: Long): SystemEntity?

    @Query("SELECT * FROM systems WHERE id = :id LIMIT 1")
    fun getSystemByIdFlow(id: Long): Flow<SystemEntity?>

    @Query("UPDATE systems SET currentStreak = :streak, lastStreakDate = :lastDate WHERE id = :systemId")
    suspend fun updateStreak(systemId: Long, streak: Int, lastDate: Long)

    @Query("UPDATE systems SET freezeMonthKey = :monthKey, freezeDaysUsedThisMonth = :daysUsed WHERE id = :systemId")
    suspend fun updateFreezeMonth(systemId: Long, monthKey: Int, daysUsed: Int)

    @Query("UPDATE systems SET currentStreak = 0, lastStreakDate = -1, freezeMonthKey = 0, freezeDaysUsedThisMonth = 0 WHERE id = :systemId")
    suspend fun resetStreak(systemId: Long)

    @Query("SELECT COALESCE(MAX(sortOrder), -1) + 1 FROM systems")
    suspend fun nextSortOrder(): Int
}
