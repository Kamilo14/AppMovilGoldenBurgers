package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddressUiState(
    val addresses: List<DireccionCliente> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletingAddressId: Long? = null // [NUEVO] Para controlar la eliminación
)

class AddressViewModel(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddressUiState())
    val uiState: StateFlow<AddressUiState> = _uiState.asStateFlow()

    private suspend fun ensureClientLoaded(): Boolean {
        if (clienteRepository.currentCliente.value != null) return true

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _uiState.update { it.copy(isLoading = false, error = "Error: Sesión de usuario no encontrada.") }
            return false
        }

        val result = clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid)
        return result.isSuccess
    }

    fun loadAddresses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (!ensureClientLoaded()) return@launch

            val currentCliente = clienteRepository.currentCliente.value
            if (currentCliente == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo encontrar la información del cliente.") }
                return@launch
            }

            // Las direcciones ya deberían estar en la caché del cliente, las leemos de ahí
            _uiState.update { it.copy(isLoading = false, addresses = currentCliente.direcciones) }
        }
    }

    fun deleteAddress(idDireccion: Long) {
        // Evitar múltiples eliminaciones simultáneas
        if (_uiState.value.deletingAddressId != null) return

        viewModelScope.launch {
            // 1. Mostrar estado de carga para esa dirección específica
            _uiState.update { it.copy(deletingAddressId = idDireccion) }

            val result = clienteRepository.eliminarDireccion(idDireccion)

            result.fold(
                onSuccess = {
                    // 2. Al tener éxito, el repositorio ya actualizó la caché.
                    // Simplemente leemos la nueva lista de la caché y limpiamos el estado de eliminación.
                    val updatedAddresses = clienteRepository.currentCliente.value?.direcciones ?: emptyList()
                    _uiState.update { it.copy(addresses = updatedAddresses, deletingAddressId = null) }
                },
                onFailure = { exception ->
                    // 3. Si falla, limpiamos el estado de eliminación y mostramos el error.
                    _uiState.update { it.copy(deletingAddressId = null, error = "Error al eliminar: ${exception.message}") }
                }
            )
        }
    }
}