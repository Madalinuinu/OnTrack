package com.example.ontrack.ui.createsystem

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.ontrack.data.local.AppDatabase

class CreateSystemViewModelFactory(
    private val database: AppDatabase
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CreateSystemViewModel::class.java)) {
            return CreateSystemViewModel(
                systemDao = database.systemDao(),
                habitDao = database.habitDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
