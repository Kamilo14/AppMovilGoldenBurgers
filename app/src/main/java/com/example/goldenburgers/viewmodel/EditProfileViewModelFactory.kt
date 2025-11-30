package com.example.goldenburgers.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

class EditProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager,
    private val context: Context // [NUEVO] Se añade el Context
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [NUEVO] Se pasa el Context al constructor del ViewModel
            return EditProfileViewModel(authRepository, clienteRepository, sessionManager, context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}