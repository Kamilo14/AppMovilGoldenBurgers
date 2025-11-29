package com.example.goldenburgers.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de UI para registro de usuario
 * Adaptado al modelo del backend con direcciones y GPS
 */
data class RegisterUiState(
    // Credenciales
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,

    // Datos personales
    val fullName: String = "",
    val phoneNumber: String = "",
    val fullNameError: String? = null,
    val phoneNumberError: String? = null,

    // Dirección (según modelo del backend)
    val idCiudad: Long? = null,  // ID de la ciudad seleccionada (1-5)
    val direccion: String = "",   // Dirección completa: "Avenida siempre viva 445"
    val alias: String = "",       // Alias opcional: "Casa", "Depto", etc.
    val ciudadError: String? = null,
    val direccionError: String? = null,

    // Foto de perfil (guardada localmente, no se envía al backend)
    val profileImageUri: String? = null,

    // Estado de carga
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false  // Para mostrar indicador cuando se usa GPS
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // Validaciones
    private fun validateFullName(name: String): String? =
        if (name.isBlank()) "El nombre no puede estar vacío"
        else if (name.length < 5) "El nombre es demasiado corto"
        else null

    private fun validatePhoneNumber(phone: String): String? =
        if (phone.isBlank()) null  // Teléfono es opcional
        else if (phone.length != 9 || !phone.all { it.isDigit() }) "Debe ser un número de 9 dígitos"
        else null

    private fun validateDireccion(direccion: String): String? =
        if (direccion.isBlank()) "La dirección es obligatoria"
        else if (direccion.length < 5) "La dirección es demasiado corta"
        else null

    // Funciones para actualizar el estado - Credenciales
    fun onEmailChange(email: String) = _uiState.update {
        it.copy(
            email = email,
            emailError = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches())
                "Correo inválido"
            else null
        )
    }

    fun onPasswordChange(password: String) = _uiState.update {
        it.copy(
            password = password,
            passwordError = if (password.length < 6) "Mínimo 6 caracteres" else null
        )
    }

    // Funciones para actualizar el estado - Datos personales
    fun onFullNameChange(name: String) = _uiState.update {
        it.copy(fullName = name, fullNameError = validateFullName(name))
    }

    fun onPhoneNumberChange(phone: String) = _uiState.update {
        it.copy(phoneNumber = phone, phoneNumberError = validatePhoneNumber(phone))
    }

    // Funciones para actualizar el estado - Dirección
    fun onCiudadChange(idCiudad: Long) = _uiState.update {
        it.copy(idCiudad = idCiudad, ciudadError = null)
    }

    fun onDireccionChange(direccion: String) = _uiState.update {
        it.copy(direccion = direccion, direccionError = validateDireccion(direccion))
    }

    fun onAliasChange(alias: String) = _uiState.update {
        it.copy(alias = alias)
    }

    // Función para actualizar foto de perfil (local)
    fun onProfileImageChange(uri: String?) = _uiState.update {
        it.copy(profileImageUri = uri)
    }

    // Función para controlar estado de carga GPS
    fun onFetchingLocationChange(isFetching: Boolean) = _uiState.update {
        it.copy(isFetchingLocation = isFetching)
    }

    /**
     * Registrar nuevo usuario con Firebase, backend y crear dirección
     */
    fun onRegisterClicked(
        clienteRepository: com.example.goldenburgers.repository.ClienteRepository,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isFormValid()) {
            onError("El formulario contiene errores o datos incompletos.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value

                // 1. Registrar usuario en Firebase y backend
                val registerResult = authRepository.registerUser(
                    email = state.email,
                    password = state.password,
                    nombreCliente = state.fullName,
                    telefonoCliente = state.phoneNumber.ifBlank { null }
                )

                registerResult.fold(
                    onSuccess = { cliente ->
                        // 2. Crear dirección si se proporcionó
                        if (state.idCiudad != null && state.direccion.isNotBlank()) {
                            viewModelScope.launch {
                                val direccionResult = clienteRepository.crearDireccion(
                                    idCliente = cliente.idCliente,
                                    idCiudad = state.idCiudad,
                                    direccion = state.direccion,
                                    alias = state.alias.ifBlank { null }
                                )

                                direccionResult.fold(
                                    onSuccess = {
                                        _uiState.update { it.copy(isLoading = false) }
                                        onSuccess()
                                    },
                                    onFailure = { exception ->
                                        _uiState.update { it.copy(isLoading = false) }
                                        // Usuario creado pero dirección falló
                                        onError("Usuario creado, pero error al guardar dirección: ${exception.message}")
                                    }
                                )
                            }
                        } else {
                            // Sin dirección, registro completo
                            _uiState.update { it.copy(isLoading = false) }
                            onSuccess()
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false) }
                        onError(exception.message ?: "Error al registrar usuario")
                    }
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false) }
                onError(e.message ?: "Ocurrió un error desconocido")
            }
        }
    }

    private fun isFormValid(): Boolean {
        val state = _uiState.value
        return state.emailError == null &&
                state.passwordError == null &&
                state.fullNameError == null &&
                state.phoneNumberError == null &&
                state.direccionError == null &&
                state.email.isNotBlank() &&
                state.password.isNotBlank() &&
                state.fullName.isNotBlank() &&
                state.direccion.isNotBlank() &&
                state.idCiudad != null
    }
}

