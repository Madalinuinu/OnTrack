package com.example.ontrack.data.local.entity

import androidx.room.TypeConverter

enum class FrequencyType {
    DAILY,
    WEEKLY,
    SPECIFIC_DAYS
}

class FrequencyTypeConverter {
    @TypeConverter
    fun fromString(value: String): FrequencyType = FrequencyType.valueOf(value)

    @TypeConverter
    fun toStorage(type: FrequencyType): String = type.name
}
