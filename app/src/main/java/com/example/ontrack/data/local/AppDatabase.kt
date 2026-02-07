package com.example.ontrack.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
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
    version = 6,
    exportSchema = false
)
@TypeConverters(FrequencyTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE systems ADD COLUMN currentStreak INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE systems ADD COLUMN lastStreakDate INTEGER NOT NULL DEFAULT -1")
            }
        }
        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE systems ADD COLUMN pausedFromEpochDay INTEGER")
                db.execSQL("ALTER TABLE systems ADD COLUMN pausedToEpochDay INTEGER")
            }
        }
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE systems ADD COLUMN freezeMonthKey INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE systems ADD COLUMN freezeDaysUsedThisMonth INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE systems ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
            }
        }
        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE habit_logs ADD COLUMN durationMinutes INTEGER")
            }
        }
    }

    abstract fun systemDao(): SystemDao
    abstract fun habitDao(): HabitDao
    abstract fun habitLogDao(): HabitLogDao
}
