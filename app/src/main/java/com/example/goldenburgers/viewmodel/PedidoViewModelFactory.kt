package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.PedidoRepository

class PedidoViewModelFactory(
    private val pedidoRepository: PedidoRepository,
    private val clienteRepository: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PedidoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PedidoViewModel(pedidoRepository, clienteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for PedidoViewModelFactory")
    }
}