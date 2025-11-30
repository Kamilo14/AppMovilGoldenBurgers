package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditAddressUiState(
    val alias: String = "",
    val direccion: String = "",
    val ciudadId: Long? = null,
    val availableCiudades: List<Ciudad> = emptyList(),
    val isNewAddress: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,
    val finishScreen: Boolean = false
)

class EditAddressViewModel(
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditAddressUiState())
    val uiState: StateFlow<EditAddressUiState> = _uiState.asStateFlow()

    // [CORREGIDO] El init ahora está vacío para que sea testeable
    init {}

    // [NUEVO] Función para ser llamada desde la UI
    fun loadInitialData() {
        loadAvailableCiudades()
    }

    private fun loadAvailableCiudades() {
        viewModelScope.launch {
            clienteRepository.obtenerCiudades().onSuccess { ciudades ->
                _uiState.update { it.copy(availableCiudades = ciudades) }
            }
        }
    }

    fun loadAddress(addressId: Long) {
        if (addressId == -1L) {
            _uiState.update { it.copy(isNewAddress = true, isLoading = false) }
            return
        }

        _uiState.update { it.copy(isLoading = true, isNewAddress = false) }

        viewModelScope.launch {
            val cliente = clienteRepository.currentCliente.value
            val addressToEdit = cliente?.direcciones?.find { it.idDireccion == addressId }

            if (addressToEdit != null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        alias = addressToEdit.alias ?: "",
                        direccion = addressToEdit.direccion,
                        ciudadId = addressToEdit.ciudad.idCiudad
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo encontrar la dirección para editar.") }
            }
        }
    }

    fun onAliasChange(newAlias: String) = _uiState.update { it.copy(alias = newAlias) }
    fun onDireccionChange(newDireccion: String) = _uiState.update { it.copy(direccion = newDireccion) }
    fun onCiudadSelected(newCiudadId: Long) = _uiState.update { it.copy(ciudadId = newCiudadId) }

    fun saveAddress(addressId: Long) {
        val state = _uiState.value
        if (state.direccion.isBlank() || state.ciudadId == null) {
            _uiState.update { it.copy(error = "La dirección y la ciudad son obligatorias.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val cliente = clienteRepository.currentCliente.firstOrNull()
            if (cliente == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener la información del cliente.") }
                return@launch
            }

            val result = if (state.isNewAddress) {
                clienteRepository.crearDireccion(
                    idCliente = cliente.idCliente,
                    idCiudad = state.ciudadId,
                    direccion = state.direccion,
                    alias = state.alias.ifBlank { null }
                )
            } else {
                clienteRepository.actualizarDireccion(
                    idDireccion = addressId,
                    idCiudad = state.ciudadId,
                    direccion = state.direccion,
                    alias = state.alias.ifBlank { null }
                )
            }

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, finishScreen = true) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
            )
        }
    }
}