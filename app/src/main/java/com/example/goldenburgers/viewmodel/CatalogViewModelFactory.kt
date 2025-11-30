package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.ClienteRepository

class CatalogViewModelFactory(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager,
    private val clienteRepository: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        // [CORRECCIÓN CLAVE] Se usa una comparación directa para asegurar que solo se cree el ViewModel correcto.
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
             @Suppress("UNCHECKED_CAST")
            return CatalogViewModel(repository, sessionManager, clienteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
