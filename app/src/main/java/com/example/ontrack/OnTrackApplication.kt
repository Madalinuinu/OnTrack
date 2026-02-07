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
