package com.example.goldenburgers.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val fullName: String = "",
    val phoneNumber: String = "",
    val fullNameError: String? = null,
    val phoneNumberError: String? = null,
    val idCiudad: Long? = null,
    val direccion: String = "",
    val alias: String = "",
    val ciudadError: String? = null,
    val direccionError: String? = null,
    val profileImageUri: String? = null,
    val isLoading: Boolean = false,
    val isFetchingLocation: Boolean = false
)

class RegisterViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    private fun validateFullName(name: String): String? =
        if (name.isBlank()) "El nombre no puede estar vacío" else if (name.length < 5) "El nombre es demasiado corto" else null

    private fun validatePhoneNumber(phone: String): String? =
        if (phone.isBlank()) null else if (phone.length != 9 || !phone.all { it.isDigit() }) "Debe ser un número de 9 dígitos" else null

    private fun validateDireccion(direccion: String): String? =
        if (direccion.isBlank()) "La dirección es obligatoria" else if (direccion.length < 5) "La dirección es demasiado corta" else null

    fun onEmailChange(email: String) = _uiState.update {
        it.copy(email = email, emailError = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Correo inválido" else null)
    }

    fun onPasswordChange(password: String) = _uiState.update {
        it.copy(password = password, passwordError = if (password.length < 6) "Mínimo 6 caracteres" else null)
    }

    fun onFullNameChange(name: String) = _uiState.update {
        it.copy(fullName = name, fullNameError = validateFullName(name))
    }

    fun onPhoneNumberChange(phone: String) = _uiState.update {
        it.copy(phoneNumber = phone, phoneNumberError = validatePhoneNumber(phone))
    }

    fun onCiudadChange(idCiudad: Long) = _uiState.update {
        it.copy(idCiudad = idCiudad, ciudadError = null)
    }

    fun onDireccionChange(direccion: String) = _uiState.update {
        it.copy(direccion = direccion, direccionError = validateDireccion(direccion))
    }

    fun onAliasChange(alias: String) = _uiState.update { it.copy(alias = alias) }

    fun onProfileImageChange(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }

    fun onFetchingLocationChange(isFetching: Boolean) = _uiState.update { it.copy(isFetchingLocation = isFetching) }

    /**
     * [CORREGIDO] Orquesta el flujo de registro completo de forma secuencial.
     */
    fun onRegisterClicked(
        clienteRepository: ClienteRepository,
        sessionManager: SessionManager,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!isFormValid()) {
            onError("El formulario contiene errores o datos incompletos.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val state = _uiState.value

            // 1. Registrar usuario y obtener token interno
            authRepository.registerUser(
                email = state.email,
                password = state.password,
                nombreCliente = state.fullName,
                telefonoCliente = state.phoneNumber.ifBlank { null }
            ).onSuccess { registrationResult ->
                val cliente = registrationResult.cliente
                val internalToken = registrationResult.internalToken

                // 2. Guardar sesión con el token INTERNO
                sessionManager.saveUserSession(state.email, internalToken)

                // 3. Crear dirección (si aplica) en la misma corutina
                if (state.idCiudad != null && state.direccion.isNotBlank()) {
                    clienteRepository.crearDireccion(
                        idCliente = cliente.idCliente,
                        idCiudad = state.idCiudad,
                        direccion = state.direccion,
                        alias = state.alias.ifBlank { null }
                    ).onSuccess {
                        _uiState.update { it.copy(isLoading = false) }
                        onSuccess()
                    }.onFailure {
                        _uiState.update { it.copy(isLoading = false) }
                        onError("Usuario creado, pero error al guardar dirección: ${it.message}")
                    }
                } else {
                    // Si no hay dirección, el registro está completo
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }
            }.onFailure {
                _uiState.update { it.copy(isLoading = false) }
                onError(it.message ?: "Error al registrar usuario")
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