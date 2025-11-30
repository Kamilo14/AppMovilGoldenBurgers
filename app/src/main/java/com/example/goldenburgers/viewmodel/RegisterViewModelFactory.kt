package com.example.goldenburgers.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

/**
 * Factory para crear instancias de RegisterViewModel
 */
// [CORRECCIÓN] Se añade el Context como dependencia para poder pasárselo al ViewModel
class RegisterViewModelFactory(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [CORRECCIÓN] Se pasa el Context al constructor del ViewModel
            return RegisterViewModel(authRepository, clienteRepository, sessionManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for RegisterViewModelFactory")
    }
}
