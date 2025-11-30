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
 * ViewModel para la pantalla de Login con Firebase Authentication
 */
class LoginViewModel(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager // Inyectamos SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        val error = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
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

    /**
     * Login con Firebase Authentication
     * Después de autenticar, guarda el token y carga la información del cliente desde el backend
     */
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (!isFormValid()) {
            onError("Por favor, corrige los errores en el formulario.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val state = _uiState.value
                // 1. Login en Firebase
                val loginResult = authRepository.login(state.email, state.password)

                loginResult.fold(
                    onSuccess = { firebaseUser ->
                        viewModelScope.launch {
                            // 2. Obtener Token JWT y guardar sesión inmediatamente
                            val token = authRepository.getAuthToken()
                            if (token != null) {
                                sessionManager.saveUserSession(state.email, token)
                                
                                // 3. Ahora sí, cargar información del cliente desde el backend (ClienteRepository leerá el token guardado)
                                val clienteResult = clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid)

                                clienteResult.fold(
                                    onSuccess = {
                                        _uiState.update { it.copy(isLoading = false) }
                                        onSuccess()
                                    },
                                    onFailure = { exception ->
                                        _uiState.update { it.copy(isLoading = false) }
                                        // Aunque falle la carga de perfil, el login fue exitoso.
                                        // Podríamos dejar pasar al usuario o mostrar error.
                                        // Mostramos error para depurar el 403 si persiste.
                                        onError("Error al cargar datos del usuario: ${exception.message}")
                                    }
                                )
                            } else {
                                _uiState.update { it.copy(isLoading = false) }
                                onError("Error al obtener token de autenticación")
                            }
                        }
                    },
                    onFailure = { exception ->
                        _uiState.update { it.copy(isLoading = false) }
                        onError(exception.message ?: "Error al iniciar sesión")
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
