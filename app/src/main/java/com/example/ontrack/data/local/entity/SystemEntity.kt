package com.example.ontrack.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "systems")
data class SystemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val goal: String,
    val duration: Int? = null,
    val startDate: Long,
    /** Display order in home list; lower = higher in list. */
    val sortOrder: Int = 0,
    val currentStreak: Int = 0,
    val lastStreakDate: Long = -1L,
    /** Pause range (e.g. vacation): epoch days inclusive. null = not paused. */
    val pausedFromEpochDay: Long? = null,
    val pausedToEpochDay: Long? = null,
    /** For "max 5 freeze days per month": month key = year*12+month (e.g. Feb 2025 = 24302). */
    val freezeMonthKey: Int = 0,
    val freezeDaysUsedThisMonth: Int = 0
)
