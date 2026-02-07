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
    val targetCount: Int = 1
)
