package com.example.ontrack

import android.app.Application
import androidx.room.Room
import com.example.ontrack.data.local.AppDatabase
import com.example.ontrack.data.preferences.UserPreferences
import com.example.ontrack.data.streak.StreakManager

class OnTrackApplication : Application() {

    val userPreferences: UserPreferences by lazy { UserPreferences(this) }

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "ontrack.db")
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3, AppDatabase.MIGRATION_3_4, AppDatabase.MIGRATION_4_5, AppDatabase.MIGRATION_5_6, AppDatabase.MIGRATION_6_7, AppDatabase.MIGRATION_7_8, AppDatabase.MIGRATION_8_9, AppDatabase.MIGRATION_9_10, AppDatabase.MIGRATION_10_11, AppDatabase.MIGRATION_11_12)
            .build()
    }

    val streakManager: StreakManager by lazy {
        StreakManager(
            systemDao = database.systemDao(),
            habitDao = database.habitDao(),
            habitLogDao = database.habitLogDao(),
            userPreferences = userPreferences
        )
    }
}
