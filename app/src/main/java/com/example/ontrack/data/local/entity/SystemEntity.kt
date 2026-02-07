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
    val startDate: Long
)
