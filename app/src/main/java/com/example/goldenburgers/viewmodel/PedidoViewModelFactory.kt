package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.PedidoRepository

// [CORREGIDO] Se añade AuthRepository como dependencia
class PedidoViewModelFactory(
    private val pedidoRepository: PedidoRepository,
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository 
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // [CORREGIDO] Se pasa la nueva dependencia al ViewModel
            return PedidoViewModel(pedidoRepository, clienteRepository, authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for PedidoViewModelFactory")
    }
}