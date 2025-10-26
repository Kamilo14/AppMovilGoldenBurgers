package com.example.goldenburgers.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.ProductRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Data class para el estado de la UI de la pantalla de Login.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null
)

/**
 * ViewModel para la pantalla de Login.
 * [ACTUALIZADO] Ahora recibe el repositorio para validar las credenciales del usuario.
 */
class LoginViewModel(private val repository: ProductRepository) : ViewModel() {

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
        val error = if (password.length < 6) "La contraseña debe tener al menos 6 caracteres" else null
        _uiState.update { it.copy(password = password, passwordError = error) }
    }

    /**
     * [NUEVO] Inicia el proceso de login.
     * Busca al usuario por email y verifica la contraseña.
     * @param onSuccess Callback que se ejecuta si el login es exitoso.
     * @param onError Callback que se ejecuta si hay un error, pasando un mensaje.
     */
    fun login(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isFormValid()) {
            viewModelScope.launch {
                val state = _uiState.value
                val user = repository.findUserByEmail(state.email)

                if (user == null) {
                    onError("Usuario no encontrado.")
                } else if (user.password != state.password) {
                    // En una app real, aquí se compararía el hash de la contraseña
                    onError("Contraseña incorrecta.")
                } else {
                    // ¡Éxito!
                    onSuccess()
                }
            }
        } else {
            onError("Por favor, corrige los errores en el formulario.")
        }
    }

    private fun isFormValid(): Boolean {
        val state = _uiState.value
        return state.email.isNotBlank() && state.password.isNotBlank() &&
                state.emailError == null && state.passwordError == null
    }
}

