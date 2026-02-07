package com.example.ontrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.ontrack.data.local.dao.HabitDao
import com.example.ontrack.data.local.dao.HabitLogDao
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.local.entity.FrequencyTypeConverter
import com.example.ontrack.data.local.entity.HabitEntity
import com.example.ontrack.data.local.entity.HabitLogEntity
import com.example.ontrack.data.local.entity.SystemEntity

@Database(
    entities = [
        SystemEntity::class,
        HabitEntity::class,
        HabitLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(FrequencyTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun systemDao(): SystemDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
}
