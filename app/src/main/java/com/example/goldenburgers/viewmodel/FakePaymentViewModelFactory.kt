package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.repository.PedidoRepository

class FakePaymentViewModelFactory(
    private val pedidoRepository: PedidoRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FakePaymentViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return FakePaymentViewModel(pedidoRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for FakePaymentViewModelFactory")
    }
}