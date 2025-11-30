package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository

class AddressViewModelFactory(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository // [NUEVO] Se recibe AuthRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [CORREGIDO] Se pasan ambas dependencias al ViewModel
            return AddressViewModel(clienteRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for AddressViewModelFactory")
    }
}