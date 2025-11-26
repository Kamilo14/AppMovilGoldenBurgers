package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

/**
 * Factory para crear instancias de EditProfileViewModel
 * Provee AuthRepository y ClienteRepository necesarios para gestión de perfil
 */
class EditProfileViewModelFactory(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(authRepository, clienteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for EditProfileViewModelFactory")
    }
}