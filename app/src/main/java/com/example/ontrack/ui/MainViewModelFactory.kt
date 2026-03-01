package com.example.ontrack.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ontrack.data.local.dao.SystemDao
import com.example.ontrack.data.preferences.UserPreferences

class MainViewModelFactory(
    private val userPreferences: UserPreferences,
    private val systemDao: SystemDao,
    private val initialPageFromIntent: Int = 0
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            return MainViewModel(userPreferences, systemDao, initialPageFromIntent) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
