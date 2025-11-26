package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

/**
 * Factory para crear instancias de LoginViewModel
 * Provee AuthRepository y ClienteRepository necesarios para login con Firebase y backend
 */
class LoginViewModelFactory(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return LoginViewModel(authRepository, clienteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for LoginViewModelFactory")
    }
}

