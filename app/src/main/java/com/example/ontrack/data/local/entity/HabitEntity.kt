package com.example.ontrack.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "habits",
    foreignKeys = [
        ForeignKey(
            entity = SystemEntity::class,
            parentColumns = ["id"],
            childColumns = ["systemId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("systemId")]
)
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val systemId: Long,
    val title: String,
    val frequencyType: FrequencyType,
    /** For "X times per week" (e.g. SPECIFIC_DAYS), targetCount = 3 means 3 times per week. */
    val targetCount: Int = 1,
    /** When true, tapping the task opens the timer; when false, tap completes directly. */
    val trackTimeEnabled: Boolean = false,
    /** Last timer duration selected for this habit, in seconds; null until user has started a timer at least once. */
    val lastTimerDurationSeconds: Int? = null,
    /**
     * First epoch day this habit counts toward streak (inclusive). Null = always counted.
     * Set to tomorrow when a habit is added via goal edit so today’s streak stays based on pre-edit habits only.
     */
    val countsForStreakFromEpochDay: Long? = null
)
