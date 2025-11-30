package com.example.goldenburgers.viewmodel

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

/**
 * Estado de la UI de la pantalla de Login
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false
)

/**
 * ViewModel para la pantalla de Login con Firebase Authentication y backend.
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    // [CORREGIDO] Se usa una regex de Kotlin puro para que el ViewModel no dependa del framework de Android.
    private val emailRegex = Regex("[a-zA-Z0-9._-]+@[a-z]+\\.+[a-z]+")

    fun onEmailChange(email: String) {
        val error = if (email.isNotBlank() && !email.matches(emailRegex)) {
            "Formato de correo inválido"
        } else {
            null
        }
        _uiState.update { it.copy(email = email, emailError = error) }
    }

    fun onPasswordChange(password: String) {
        val error = if (password.isNotBlank() && password.length < 6) {
            "La contraseña debe tener al menos 6 caracteres"
        } else {
            null
        }
        _uiState.update { it.copy(password = password, passwordError = error) }
    }

    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isFormValid()) {
            onError("Por favor, corrige los errores en el formulario.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                val loginResult = authRepository.login(state.email, state.password)

                loginResult.fold(
                    onSuccess = { firebaseUser ->
                        val firebaseToken = authRepository.getFirebaseToken()
                        if (firebaseToken == null) {
                            onError("No se pudo obtener el token de Firebase.")
                            _uiState.update { it.copy(isLoading = false) }
                            return@fold
                        }

                        val exchangeResult = authRepository.exchangeToken(firebaseToken)
                        exchangeResult.fold(
                            onSuccess = { internalToken ->
                                sessionManager.saveUserSession(state.email, internalToken)

                                val clienteResult = clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid)
                                clienteResult.fold(
                                    onSuccess = {
                                        _uiState.update { it.copy(isLoading = false) }
                                        onSuccess()
                                    },
                                    onFailure = { e ->
                                        _uiState.update { it.copy(isLoading = false) }
                                        onError("Login exitoso, pero no se pudieron cargar los datos del perfil: ${e.message}")
                                    }
                                )
                            },
                            onFailure = { e ->
                                _uiState.update { it.copy(isLoading = false) }
                                onError("Error de autenticación con el servidor: ${e.message}")
                            }
                        )
                    },
                    onFailure = { e ->
                        _uiState.update { it.copy(isLoading = false) }
                        onError(e.message ?: "Error al iniciar sesión")
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
        return state.email.isNotBlank() &&
                state.password.isNotBlank() &&
                state.emailError == null &&
                state.passwordError == null
    }
}