package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

class EditAddressViewModelFactory(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditAddressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditAddressViewModel(clienteRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for EditAddressViewModelFactory")
    }
}