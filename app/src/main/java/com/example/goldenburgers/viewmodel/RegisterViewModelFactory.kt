package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository

/**
 * Factory para crear instancias de RegisterViewModel
 * Provee el AuthRepository necesario para registro con Firebase y backend
 */
class RegisterViewModelFactory(
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return RegisterViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RegisterViewModelFactory")
    }
}

