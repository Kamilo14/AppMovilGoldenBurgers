package com.example.goldenburgers.viewmodel



import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


/**
 * Estado de la UI para la pantalla de edición de perfil.
 */
data class EditProfileUiState(
    val fullName: String = "",
    val phoneNumber: String = "",
    val street: String = "",
    val number: String = "",
    val city: String = "",
    val commune: String = "",
    val region: String = "",
    val profileImageUri: String? = null,
    val isLoading: Boolean = true,
    val user: User? = null
)

/**
 * Añadida una función pública para recargar los datos.
 */
class EditProfileViewModel(
    private val repository: ProductRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    // El init ya no carga los datos, se hará desde la pantalla.

    /**
     *  Carga o recarga los datos del usuario actual.
     * Es pública para que la UI pueda llamarla cuando sea necesario.
     */
    fun loadCurrentUser() {
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            val userEmail = sessionManager.loggedInUserEmailFlow.first()
            if (userEmail != null) {
                val user = repository.findUserByEmail(userEmail)
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            user = user,
                            fullName = user.fullName,
                            phoneNumber = user.phoneNumber,
                            street = user.street,
                            number = user.number,
                            city = user.city,
                            commune = user.commune,
                            region = user.region,
                            profileImageUri = user.profileImageUri,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false) }
                }
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    //  Funciones para actualizar los campos del formulario
    fun onFullNameChange(name: String) = _uiState.update { it.copy(fullName = name) }
    fun onPhoneNumberChange(phone: String) = _uiState.update { it.copy(phoneNumber = phone) }
    fun onStreetChange(street: String) = _uiState.update { it.copy(street = street) }
    fun onNumberChange(number: String) = _uiState.update { it.copy(number = number) }
    fun onCityChange(city: String) = _uiState.update { it.copy(city = city) }
    fun onCommuneChange(commune: String) = _uiState.update { it.copy(commune = commune) }
    fun onRegionChange(region: String) = _uiState.update { it.copy(region = region) }
    fun onProfileImageChange(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }

    fun saveChanges(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        val originalUser = currentState.user

        if (originalUser != null) {
            val updatedUser = originalUser.copy(
                fullName = currentState.fullName,
                phoneNumber = currentState.phoneNumber,
                street = currentState.street,
                number = currentState.number,
                city = currentState.city,
                commune = currentState.commune,
                region = currentState.region,
                profileImageUri = currentState.profileImageUri
            )

            viewModelScope.launch {
                try {
                    repository.updateUser(updatedUser)
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.message ?: "Error al guardar los cambios")
                }
            }
        } else {
            onError("No se pudo encontrar al usuario original para guardar los cambios.")
        }
    }
}
