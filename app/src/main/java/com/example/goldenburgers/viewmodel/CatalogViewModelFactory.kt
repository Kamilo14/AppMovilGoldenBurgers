package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.ClienteRepository

/**
 * [CORREGIDO] Factory para crear instancias de CatalogViewModel.
 * Ahora también provee el ClienteRepository.
 */
class CatalogViewModelFactory(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager,
    private val clienteRepository: ClienteRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Se pasan las tres dependencias al constructor del ViewModel.
            return CatalogViewModel(repository, sessionManager, clienteRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for CatalogViewModelFactory")
    }
}
