package com.example.ontrack.ui.createsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ontrack.data.local.AppDatabase
import com.example.ontrack.data.preferences.UserPreferences

class CreateSystemViewModelFactory(
    private val database: AppDatabase,
    private val userPreferences: UserPreferences
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateSystemViewModel::class.java)) {
            return CreateSystemViewModel(
                systemDao = database.systemDao(),
                habitDao = database.habitDao(),
                userPreferences = userPreferences
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
