package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

/**
 * Factory para crear instancias de RegisterViewModel
 */
// [CORREGIDO] Se añaden las nuevas dependencias que necesita el ViewModel
class RegisterViewModelFactory(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [CORREGIDO] Se pasan las nuevas dependencias al constructor del ViewModel
            return RegisterViewModel(authRepository, clienteRepository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RegisterViewModelFactory")
    }
}
