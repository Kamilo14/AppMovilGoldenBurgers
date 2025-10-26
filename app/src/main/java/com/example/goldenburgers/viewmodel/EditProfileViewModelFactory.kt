package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager

/**
 * Factory para crear instancias de EditProfileViewModel.
 * Proporciona el ProductRepository y el SessionManager necesarios.
 */
class EditProfileViewModelFactory(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditProfileViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditProfileViewModel(repository, sessionManager) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class for EditProfileViewModelFactory")
    }
}