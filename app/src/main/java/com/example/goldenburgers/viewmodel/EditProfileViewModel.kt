package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la UI para la pantalla de edición de perfil
 */
data class EditProfileUiState(
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    val email: String = "",
    val profileImageUri: String? = null,  // Foto de perfil local (no se guarda en backend)
    val isLoading: Boolean = true,
    val cliente: Cliente? = null
)

/**
 * ViewModel para edición de perfil con API backend
 */
class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager // Inyectamos SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    /**
     * [CORREGIDO] Cargar datos del cliente actual desde el backend.
     * La lógica de manejo de tokens es innecesaria aquí, ya que el ClienteRepository
     * ya está configurado para usar el token guardado en SessionManager.
     */
    fun loadCurrentUser() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val firebaseUser = authRepository.getCurrentUser()

            if (firebaseUser != null) {
                // El ClienteRepository se encargará de usar el token de sesión correcto.
                val result = clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid)

                result.fold(
                    onSuccess = { cliente ->
                        _uiState.update {
                            it.copy(
                                cliente = cliente,
                                nombreCliente = cliente.nombreCliente,
                                telefonoCliente = cliente.telefonoCliente ?: "",
                                email = cliente.usuario.email,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = {
                        // Aquí podrías mostrar un mensaje de error si lo deseas
                        _uiState.update { it.copy(isLoading = false) }
                    }
                )
            } else {
                // No hay usuario logueado
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    // --- Funciones para actualizar los campos del formulario ---
    fun onNombreClienteChange(nombre: String) = _uiState.update {
        it.copy(nombreCliente = nombre)
    }

    fun onTelefonoClienteChange(telefono: String) = _uiState.update {
        it.copy(telefonoCliente = telefono)
    }

    fun onProfileImageChange(uri: String?) = _uiState.update {
        it.copy(profileImageUri = uri)
    }

    /**
     * Guardar cambios del perfil
     */
    fun saveChanges(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        val cliente = currentState.cliente

        if (cliente == null) {
            onError("No se pudo encontrar la información del cliente.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val result = clienteRepository.actualizarPerfil(
                nombreCliente = currentState.nombreCliente,
                email = currentState.email,
                telefonoCliente = currentState.telefonoCliente.ifBlank { null }
            )

            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    onError(exception.message ?: "Error al guardar los cambios")
                }
            )
        }
    }
}