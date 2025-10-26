package com.example.goldenburgers.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * [ACTUALIZADO] Añadida función para controlar el estado de carga del GPS.
 */
data class RegisterUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val fullName: String = "",
    val phoneNumber: String = "",
    val gender: String = "",
    val birthDate: String = "",
    val fullNameError: String? = null,
    val phoneNumberError: String? = null,
    val genderError: String? = null,
    val birthDateError: String? = null,
    val profileImageUri: String? = null,
    val number: String = "",
    val street: String = "",
    val commune: String = "",
    val city: String = "",
    val region: String = "",
    val streetError: String? = null,
    val numberError: String? = null,
    val communeError: String? = null,
    val cityError: String? = null,
    val regionError: String? = null,
    val isFetchingLocation: Boolean = false
)

class RegisterViewModel(private val repository: ProductRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // [NUEVO] Función para que la UI pueda notificar el cambio de estado de la carga.
    fun onFetchingLocationChange(isFetching: Boolean) {
        _uiState.update { it.copy(isFetchingLocation = isFetching) }
    }

    // ... (El resto del ViewModel no cambia)
    private fun validateFullName(name: String): String? = if (name.isBlank()) "El nombre no puede estar vacío" else if (name.length < 5) "El nombre es demasiado corto" else null
    private fun validatePhoneNumber(phone: String): String? = if (phone.isBlank()) "El teléfono es obligatorio" else if (phone.length != 9 || !phone.all { it.isDigit() }) "Debe ser un número de 9 dígitos" else null
    private fun validateGender(gender: String): String? = if (gender.isBlank()) "El género es obligatorio" else null
    private fun validateBirthDate(date: String): String? = if (!"^\\d{4}-\\d{2}-\\d{2}$".toRegex().matches(date)) "El formato debe ser AAAA-MM-DD" else null
    private fun validateNumber(number: String): String? = if (number.isBlank()) "El número es obligatorio" else if (!number.all { it.isDigit() }) "Solo números" else null

    fun onEmailChange(email: String) = _uiState.update { it.copy(email = email, emailError = if (email.isNotBlank() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()) "Correo inválido" else null) }
    fun onPasswordChange(password: String) = _uiState.update { it.copy(password = password, passwordError = if (password.length < 6) "Mínimo 6 caracteres" else null) }
    fun onFullNameChange(name: String) = _uiState.update { it.copy(fullName = name, fullNameError = validateFullName(name)) }
    fun onPhoneNumberChange(phone: String) = _uiState.update { it.copy(phoneNumber = phone, phoneNumberError = validatePhoneNumber(phone)) }
    fun onGenderChange(gender: String) = _uiState.update { it.copy(gender = gender, genderError = validateGender(gender)) }
    fun onBirthDateChange(date: String) = _uiState.update { it.copy(birthDate = date, birthDateError = validateBirthDate(date)) }
    fun onProfileImageChange(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }
    fun onStreetChange(street: String) = _uiState.update { it.copy(street = street, streetError = if (street.isBlank()) "La calle es obligatoria" else null) }
    fun onNumberChange(number: String) = _uiState.update { it.copy(number = number, numberError = validateNumber(number)) }
    fun onCommuneChange(commune: String) = _uiState.update { it.copy(commune = commune, communeError = if (commune.isBlank()) "La comuna es obligatoria" else null) }
    fun onCityChange(city: String) = _uiState.update { it.copy(city = city, cityError = if (city.isBlank()) "La ciudad es obligatoria" else null) }
    fun onRegionChange(region: String) = _uiState.update { it.copy(region = region, regionError = if (region.isBlank()) "La región es obligatoria" else null) }

    fun onRegisterClicked(onSuccess: () -> Unit, onError: (String) -> Unit) {
        if (isFormValid()) {
            viewModelScope.launch {
                try {
                    val state = _uiState.value
                    val newUser = User(
                        email = state.email,
                        password = state.password,
                        fullName = state.fullName,
                        phoneNumber = state.phoneNumber,
                        gender = state.gender,
                        birthDate = state.birthDate,
                        street = state.street,
                        number = state.number,
                        city = state.city,
                        region = state.region,
                        commune = state.commune,
                        profileImageUri = state.profileImageUri
                    )
                    repository.registerUser(newUser)
                    onSuccess()
                } catch (e: Exception) {
                    onError(e.message ?: "Ocurrió un error desconocido")
                }
            }
        } else {
            onError("El formulario contiene errores o datos incompletos.")
        }
    }

    private fun isFormValid(): Boolean {
        val state = _uiState.value
        return state.emailError == null && state.passwordError == null &&
                state.fullNameError == null && state.phoneNumberError == null &&
                state.genderError == null && state.birthDateError == null &&
                state.streetError == null && state.numberError == null &&
                state.communeError == null && state.cityError == null && state.regionError == null &&
                state.email.isNotBlank() && state.password.isNotBlank() &&
                state.fullName.isNotBlank() && state.phoneNumber.isNotBlank() &&
                state.gender.isNotBlank() && state.birthDate.isNotBlank() &&
                state.street.isNotBlank() && state.number.isNotBlank() &&
                state.commune.isNotBlank() && state.city.isNotBlank() &&
                state.region.isNotBlank()
    }
}

