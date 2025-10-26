package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager


/**
 * Factory para crear instancias de CatalogViewModel.
 * [ACTUALIZADO] Ahora también provee el SessionManager.
 */
class CatalogViewModelFactory(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager // <-- AÑADIDO
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            // Se pasan ambas dependencias al constructor del ViewModel.
            return CatalogViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for CatalogViewModelFactory")
    }
}
