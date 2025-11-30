package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.RoutingRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AddressUiState(
    val addresses: List<DireccionCliente> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val deletingAddressId: Long? = null,
    val estimatedDeliveryTime: Int? = null,
    val isCalculatingTime: Boolean = false
)

class AddressViewModel(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository,
    private val routingRepository: RoutingRepository
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

            _uiState.update { it.copy(isLoading = false, addresses = currentCliente.direcciones) }
        }
    }

    fun calculateDeliveryTime(address: DireccionCliente) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalculatingTime = true, estimatedDeliveryTime = null) }
            val fullAddress = "${address.direccion}, ${address.ciudad.nombreCiudad}"
            val time = routingRepository.getEstimatedDeliveryTime(fullAddress)
            _uiState.update { it.copy(isCalculatingTime = false, estimatedDeliveryTime = time) }
        }
    }

    // [NUEVO] Función para resetear el tiempo de entrega
    fun resetDeliveryTime() {
        _uiState.update { it.copy(isCalculatingTime = false, estimatedDeliveryTime = null) }
    }

    fun deleteAddress(idDireccion: Long) {
        if (_uiState.value.deletingAddressId != null) return

        viewModelScope.launch {
            _uiState.update { it.copy(deletingAddressId = idDireccion) }

            val result = clienteRepository.eliminarDireccion(idDireccion)

            result.fold(
                onSuccess = {
                    val updatedAddresses = clienteRepository.currentCliente.value?.direcciones ?: emptyList()
                    _uiState.update { it.copy(addresses = updatedAddresses, deletingAddressId = null) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(deletingAddressId = null, error = "Error al eliminar: ${exception.message}") }
                }
            )
        }
    }
}