package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.RoutingRepository

// [CORREGIDO] Se añade RoutingRepository como dependencia
class AddressViewModelFactory(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository,
    private val routingRepository: RoutingRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddressViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [CORREGIDO] Se pasa la nueva dependencia al ViewModel
            return AddressViewModel(clienteRepository, authRepository, routingRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}