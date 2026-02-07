package com.example.ontrack.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.ontrack.data.local.entity.SystemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemDao {

    @Insert
    suspend fun insertSystem(system: SystemEntity): Long

    @Query("SELECT * FROM systems ORDER BY startDate DESC")
    fun getAllSystems(): Flow<List<SystemEntity>>

    @Query("SELECT * FROM systems WHERE id = :id LIMIT 1")
    suspend fun getSystemById(id: Long): SystemEntity?
}
