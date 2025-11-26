# GUÍA DE INTEGRACIÓN - PARTE 2: AUTENTICACIÓN Y CATÁLOGO

## Golden Burgers - App Android + Backend Microservicios

---

## ÍNDICE PARTE 2

1. [Paso 8: Implementar Registro con Firebase Auth](#paso-8-implementar-registro-con-firebase-auth)
2. [Paso 9: Implementar Login con Firebase Auth](#paso-9-implementar-login-con-firebase-auth)
3. [Paso 10: Gestionar Sesión y Tokens](#paso-10-gestionar-sesión-y-tokens)
4. [Paso 11: Implementar Carga de Productos desde Backend](#paso-11-implementar-carga-de-productos-desde-backend)
5. [Paso 12: Implementar Categorías](#paso-12-implementar-categorías)
6. [Paso 13: Implementar Favoritos Locales por Usuario](#paso-13-implementar-favoritos-locales-por-usuario)
7. [Paso 14: Implementar Edición de Perfil](#paso-14-implementar-edición-de-perfil)
8. [Paso 15: Implementar Gestión de Direcciones](#paso-15-implementar-gestión-de-direcciones)
9. [Paso 16: Mantener Foto de Perfil Local](#paso-16-mantener-foto-de-perfil-local)

---

## PASO 8: IMPLEMENTAR REGISTRO CON FIREBASE AUTH

### Flujo de Registro Actualizado

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE REGISTRO                            │
├─────────────────────────────────────────────────────────────────┤
│  1. Usuario ingresa email + password (Step 1)                   │
│                          ↓                                      │
│  2. Usuario ingresa nombre + teléfono + dirección (Step 2)      │
│                          ↓                                      │
│  3. Usuario toma foto de perfil (Step 3) - OPCIONAL             │
│                          ↓                                      │
│  4. Usuario ingresa género + fecha nacimiento (Step 4) - OPC.   │
│                          ↓                                      │
│  5. Usuario confirma datos (Step 5)                             │
│                          ↓                                      │
│  6. Firebase Auth: createUserWithEmailAndPassword()             │
│                          ↓                                      │
│  7. Obtener Firebase UID del usuario creado                     │
│                          ↓                                      │
│  8. Backend: POST /api/clientes (registrar en BD Oracle)        │
│                          ↓                                      │
│  9. Backend: POST /api/clientes/direcciones (guardar dirección) │
│                          ↓                                      │
│ 10. Room: Guardar datos locales (foto, género, birthDate)       │
│                          ↓                                      │
│ 11. Obtener JWT del backend (POST /auth/login)                  │
│                          ↓                                      │
│ 12. Guardar sesión en DataStore                                 │
│                          ↓                                      │
│ 13. Navegar a MainScreen                                        │
└─────────────────────────────────────────────────────────────────┘
```

### Archivo: `data/repository/AuthRepository.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.data.repository

import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.remote.ApiService
import com.example.goldenburgers.data.remote.dto.request.*
import com.example.goldenburgers.data.remote.dto.response.*
import com.example.goldenburgers.model.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repository para operaciones de autenticación.
 *
 * Coordina:
 * - Firebase Auth (creación/login de usuarios)
 * - Backend API (registro en BD, obtención de JWT)
 * - Room (datos locales)
 * - DataStore (sesión)
 */
class AuthRepository(
    private val sessionManager: SessionManager,
    private val userLocalDao: UserLocalDao,
    private val favoriteProductDao: FavoriteProductDao
) {
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val apiService: ApiService by lazy {
        ApiClient.getApiService(sessionManager)
    }

    /**
     * Resultado sellado para operaciones de auth.
     */
    sealed class AuthResult<out T> {
        data class Success<T>(val data: T) : AuthResult<T>()
        data class Error(val message: String, val exception: Exception? = null) : AuthResult<Nothing>()
        object Loading : AuthResult<Nothing>()
    }

    // ============================================
    // REGISTRO
    // ============================================

    /**
     * Registra un nuevo usuario.
     *
     * @param email Email del usuario
     * @param password Contraseña (mínimo 6 caracteres)
     * @param nombreCliente Nombre completo
     * @param telefono Teléfono (9 dígitos, opcional)
     * @param street Calle
     * @param number Número
     * @param commune Comuna
     * @param city Ciudad
     * @param region Región
     * @param profileImageUri URI de foto de perfil (local, opcional)
     * @param gender Género (opcional)
     * @param birthDate Fecha nacimiento YYYY-MM-DD (opcional)
     */
    suspend fun registrarUsuario(
        email: String,
        password: String,
        nombreCliente: String,
        telefono: String?,
        street: String,
        number: String,
        commune: String,
        city: String,
        region: String,
        profileImageUri: String? = null,
        gender: String? = null,
        birthDate: String? = null
    ): AuthResult<ClienteResponse> = withContext(Dispatchers.IO) {
        try {
            // PASO 1: Crear usuario en Firebase Auth
            val firebaseResult = firebaseAuth
                .createUserWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = firebaseResult.user
                ?: return@withContext AuthResult.Error("Error al crear usuario en Firebase")

            val firebaseUid = firebaseUser.uid

            // PASO 2: Registrar cliente en backend
            val registrarRequest = RegistrarClienteRequest(
                idUsuario = firebaseUid,
                email = email,
                nombreCliente = nombreCliente,
                telefonoCliente = if (!telefono.isNullOrBlank()) telefono else null
            )

            val clienteResponse = apiService.registrarCliente(registrarRequest)

            if (!clienteResponse.isSuccessful) {
                // Si falla el backend, eliminar usuario de Firebase
                firebaseUser.delete().await()
                val errorBody = clienteResponse.errorBody()?.string()
                return@withContext AuthResult.Error(
                    "Error al registrar en servidor: ${clienteResponse.code()} - $errorBody"
                )
            }

            val cliente = clienteResponse.body()
                ?: return@withContext AuthResult.Error("Respuesta vacía del servidor")

            // PASO 3: Crear dirección en backend
            val direccionConcatenada = buildString {
                append("$street $number")
                if (commune.isNotBlank()) append(", $commune")
                if (city.isNotBlank()) append(", $city")
                if (region.isNotBlank()) append(", $region")
            }

            // Obtener idCiudad (buscar por nombre de ciudad)
            val ciudadResponse = apiService.getCiudadPorNombre(city)
            val idCiudad = if (ciudadResponse.isSuccessful) {
                ciudadResponse.body()?.idCiudad ?: 1L // Default si no encuentra
            } else {
                1L // Ciudad por defecto
            }

            val direccionRequest = CrearDireccionRequest(
                idCliente = cliente.idCliente,
                idCiudad = idCiudad,
                direccion = direccionConcatenada,
                alias = "Casa"
            )

            // Intentar crear dirección (no crítico si falla)
            try {
                apiService.agregarDireccion(direccionRequest)
            } catch (e: Exception) {
                // Log pero continuar
                e.printStackTrace()
            }

            // PASO 4: Guardar datos locales en Room
            val userLocal = UserLocal(
                firebaseUid = firebaseUid,
                email = email,
                profileImageUri = profileImageUri,
                gender = gender,
                birthDate = birthDate,
                street = street,
                number = number,
                commune = commune,
                city = city,
                region = region
            )
            userLocalDao.insertOrUpdate(userLocal)

            // PASO 5: Obtener JWT del backend
            val idToken = firebaseUser.getIdToken(true).await().token
                ?: return@withContext AuthResult.Error("Error al obtener token de Firebase")

            val loginRequest = LoginRequest(firebaseToken = idToken)
            val authResponse = apiService.login(loginRequest)

            if (!authResponse.isSuccessful) {
                return@withContext AuthResult.Error("Error al autenticar: ${authResponse.code()}")
            }

            val jwtToken = authResponse.body()?.token
                ?: return@withContext AuthResult.Error("Token JWT no recibido")

            // PASO 6: Guardar sesión
            sessionManager.saveSession(
                email = email,
                firebaseUid = firebaseUid,
                jwtToken = jwtToken,
                clientId = cliente.idCliente,
                clientName = cliente.nombreCliente
            )

            AuthResult.Success(cliente)

        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error(
                message = mapFirebaseError(e),
                exception = e
            )
        }
    }

    // ============================================
    // LOGIN
    // ============================================

    /**
     * Inicia sesión con email y contraseña.
     */
    suspend fun login(
        email: String,
        password: String
    ): AuthResult<ClienteResponse> = withContext(Dispatchers.IO) {
        try {
            // PASO 1: Autenticar en Firebase
            val firebaseResult = firebaseAuth
                .signInWithEmailAndPassword(email, password)
                .await()

            val firebaseUser = firebaseResult.user
                ?: return@withContext AuthResult.Error("Error de autenticación")

            val firebaseUid = firebaseUser.uid

            // PASO 2: Obtener ID Token de Firebase
            val idToken = firebaseUser.getIdToken(true).await().token
                ?: return@withContext AuthResult.Error("Error al obtener token")

            // PASO 3: Obtener JWT del backend
            val loginRequest = LoginRequest(firebaseToken = idToken)
            val authResponse = apiService.login(loginRequest)

            if (!authResponse.isSuccessful) {
                return@withContext AuthResult.Error(
                    "Error del servidor: ${authResponse.code()}"
                )
            }

            val jwtToken = authResponse.body()?.token
                ?: return@withContext AuthResult.Error("Token no recibido")

            // PASO 4: Obtener datos del cliente
            val clienteResponse = apiService.getClientePorUsuario(firebaseUid)

            if (!clienteResponse.isSuccessful) {
                return@withContext AuthResult.Error(
                    "Error al obtener datos: ${clienteResponse.code()}"
                )
            }

            val cliente = clienteResponse.body()
                ?: return@withContext AuthResult.Error("Cliente no encontrado")

            // PASO 5: Guardar sesión
            sessionManager.saveSession(
                email = email,
                firebaseUid = firebaseUid,
                jwtToken = jwtToken,
                clientId = cliente.idCliente,
                clientName = cliente.nombreCliente
            )

            AuthResult.Success(cliente)

        } catch (e: Exception) {
            e.printStackTrace()
            AuthResult.Error(
                message = mapFirebaseError(e),
                exception = e
            )
        }
    }

    // ============================================
    // LOGOUT
    // ============================================

    /**
     * Cierra sesión.
     */
    suspend fun logout() = withContext(Dispatchers.IO) {
        try {
            // Cerrar sesión en Firebase
            firebaseAuth.signOut()

            // Limpiar sesión local
            sessionManager.clearUserSession()

            // Resetear cliente API (limpia token)
            ApiClient.reset()

        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ============================================
    // HELPERS
    // ============================================

    /**
     * Verifica si hay sesión activa.
     */
    fun isLoggedIn(): Boolean {
        return firebaseAuth.currentUser != null
    }

    /**
     * Obtiene el usuario de Firebase actual.
     */
    fun getCurrentFirebaseUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Mapea errores de Firebase a mensajes amigables.
     */
    private fun mapFirebaseError(exception: Exception): String {
        val message = exception.message ?: "Error desconocido"

        return when {
            message.contains("email-already-in-use", ignoreCase = true) ->
                "Este correo ya está registrado"

            message.contains("invalid-email", ignoreCase = true) ->
                "El correo electrónico no es válido"

            message.contains("weak-password", ignoreCase = true) ->
                "La contraseña es muy débil (mínimo 6 caracteres)"

            message.contains("user-not-found", ignoreCase = true) ->
                "No existe una cuenta con este correo"

            message.contains("wrong-password", ignoreCase = true) ->
                "Contraseña incorrecta"

            message.contains("too-many-requests", ignoreCase = true) ->
                "Demasiados intentos. Intenta más tarde"

            message.contains("network", ignoreCase = true) ->
                "Error de conexión. Verifica tu internet"

            else -> "Error: $message"
        }
    }
}
```

### Actualizar: `viewmodel/RegisterViewModel.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del registro.
 */
data class RegisterUiState(
    // Step 1: Credenciales
    val email: String = "",
    val password: String = "",

    // Step 2: Datos personales
    val fullName: String = "",
    val phoneNumber: String = "",

    // Step 2: Dirección
    val street: String = "",
    val number: String = "",
    val commune: String = "",
    val city: String = "",
    val region: String = "",

    // Step 3: Foto
    val profileImageUri: String? = null,

    // Step 4: Opcionales
    val gender: String = "",
    val birthDate: String = "",

    // Errores de validación
    val emailError: String? = null,
    val passwordError: String? = null,
    val fullNameError: String? = null,
    val phoneError: String? = null,
    val streetError: String? = null,
    val numberError: String? = null,
    val communeError: String? = null,
    val cityError: String? = null,
    val regionError: String? = null,

    // Estados de carga
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isRegistrationComplete: Boolean = false
)

/**
 * ViewModel para el flujo de registro.
 */
class RegisterViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState: StateFlow<RegisterUiState> = _uiState.asStateFlow()

    // ============================================
    // ACTUALIZADORES DE CAMPOS
    // ============================================

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null) }
    }

    fun onFullNameChange(name: String) {
        _uiState.update { it.copy(fullName = name, fullNameError = null) }
    }

    fun onPhoneNumberChange(phone: String) {
        // Solo permitir dígitos, máximo 9
        val filtered = phone.filter { it.isDigit() }.take(9)
        _uiState.update { it.copy(phoneNumber = filtered, phoneError = null) }
    }

    fun onStreetChange(street: String) {
        _uiState.update { it.copy(street = street, streetError = null) }
    }

    fun onNumberChange(number: String) {
        _uiState.update { it.copy(number = number, numberError = null) }
    }

    fun onCommuneChange(commune: String) {
        _uiState.update { it.copy(commune = commune, communeError = null) }
    }

    fun onCityChange(city: String) {
        _uiState.update { it.copy(city = city, cityError = null) }
    }

    fun onRegionChange(region: String) {
        _uiState.update { it.copy(region = region, regionError = null) }
    }

    fun onProfileImageUriChange(uri: String?) {
        _uiState.update { it.copy(profileImageUri = uri) }
    }

    fun onGenderChange(gender: String) {
        _uiState.update { it.copy(gender = gender) }
    }

    fun onBirthDateChange(date: String) {
        _uiState.update { it.copy(birthDate = date) }
    }

    /**
     * Actualiza dirección desde GPS.
     */
    fun onAddressFromGps(
        street: String,
        number: String,
        commune: String,
        city: String,
        region: String
    ) {
        _uiState.update {
            it.copy(
                street = street,
                number = number,
                commune = commune,
                city = city,
                region = region,
                streetError = null,
                numberError = null,
                communeError = null,
                cityError = null,
                regionError = null
            )
        }
    }

    // ============================================
    // VALIDACIONES
    // ============================================

    fun validateStep1(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validar email
        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "El email es requerido") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email inválido") }
            isValid = false
        }

        // Validar password
        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Mínimo 6 caracteres") }
            isValid = false
        }

        return isValid
    }

    fun validateStep2(): Boolean {
        val state = _uiState.value
        var isValid = true

        // Validar nombre
        if (state.fullName.length < 5) {
            _uiState.update { it.copy(fullNameError = "Mínimo 5 caracteres") }
            isValid = false
        }

        // Validar teléfono (opcional pero si hay, debe ser 9 dígitos)
        if (state.phoneNumber.isNotBlank() && state.phoneNumber.length != 9) {
            _uiState.update { it.copy(phoneError = "Debe tener 9 dígitos") }
            isValid = false
        }

        // Validar dirección
        if (state.street.isBlank()) {
            _uiState.update { it.copy(streetError = "La calle es requerida") }
            isValid = false
        }
        if (state.number.isBlank()) {
            _uiState.update { it.copy(numberError = "El número es requerido") }
            isValid = false
        }
        if (state.commune.isBlank()) {
            _uiState.update { it.copy(communeError = "La comuna es requerida") }
            isValid = false
        }
        if (state.city.isBlank()) {
            _uiState.update { it.copy(cityError = "La ciudad es requerida") }
            isValid = false
        }
        if (state.region.isBlank()) {
            _uiState.update { it.copy(regionError = "La región es requerida") }
            isValid = false
        }

        return isValid
    }

    // ============================================
    // REGISTRO
    // ============================================

    /**
     * Ejecuta el registro completo.
     *
     * @param onSuccess Callback cuando el registro es exitoso
     * @param onError Callback cuando hay error
     */
    fun register(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.registrarUsuario(
                email = state.email,
                password = state.password,
                nombreCliente = state.fullName,
                telefono = state.phoneNumber.ifBlank { null },
                street = state.street,
                number = state.number,
                commune = state.commune,
                city = state.city,
                region = state.region,
                profileImageUri = state.profileImageUri,
                gender = state.gender.ifBlank { null },
                birthDate = state.birthDate.ifBlank { null }
            )

            when (result) {
                is AuthRepository.AuthResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRegistrationComplete = true
                        )
                    }
                    onSuccess()
                }

                is AuthRepository.AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    onError(result.message)
                }

                is AuthRepository.AuthResult.Loading -> {
                    // No hacer nada, ya está en loading
                }
            }
        }
    }

    /**
     * Limpia el mensaje de error.
     */
    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    /**
     * Resetea el estado completo.
     */
    fun resetState() {
        _uiState.value = RegisterUiState()
    }
}
```

### Archivo: `viewmodel/RegisterViewModelFactory.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.data.repository.AuthRepository
import com.example.goldenburgers.model.GoldenBurgersDatabase
import com.example.goldenburgers.model.SessionManager

/**
 * Factory para crear RegisterViewModel con dependencias.
 */
class RegisterViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RegisterViewModel::class.java)) {
            val sessionManager = SessionManager(context)
            val database = GoldenBurgersDatabase.getDatabase(context)

            val authRepository = AuthRepository(
                sessionManager = sessionManager,
                userLocalDao = database.userLocalDao(),
                favoriteProductDao = database.favoriteProductDao()
            )

            return RegisterViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## PASO 9: IMPLEMENTAR LOGIN CON FIREBASE AUTH

### Actualizar: `viewmodel/LoginViewModel.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado UI del login.
 */
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

/**
 * ViewModel para login.
 */
class LoginViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = null, errorMessage = null) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update { it.copy(password = password, passwordError = null, errorMessage = null) }
    }

    /**
     * Valida los campos antes de login.
     */
    private fun validate(): Boolean {
        val state = _uiState.value
        var isValid = true

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "El email es requerido") }
            isValid = false
        } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Email inválido") }
            isValid = false
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "La contraseña es requerida") }
            isValid = false
        }

        return isValid
    }

    /**
     * Ejecuta el login.
     */
    fun login(
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!validate()) return

        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = authRepository.login(
                email = state.email,
                password = state.password
            )

            when (result) {
                is AuthRepository.AuthResult.Success -> {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                }

                is AuthRepository.AuthResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                    onError(result.message)
                }

                is AuthRepository.AuthResult.Loading -> {
                    // Ya está en loading
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

### Archivo: `viewmodel/LoginViewModelFactory.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.goldenburgers.data.repository.AuthRepository
import com.example.goldenburgers.model.GoldenBurgersDatabase
import com.example.goldenburgers.model.SessionManager

/**
 * Factory para crear LoginViewModel con dependencias.
 */
class LoginViewModelFactory(
    private val context: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(LoginViewModel::class.java)) {
            val sessionManager = SessionManager(context)
            val database = GoldenBurgersDatabase.getDatabase(context)

            val authRepository = AuthRepository(
                sessionManager = sessionManager,
                userLocalDao = database.userLocalDao(),
                favoriteProductDao = database.favoriteProductDao()
            )

            return LoginViewModel(authRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

---

## PASO 10: GESTIONAR SESIÓN Y TOKENS

### Actualizar: `view/ProfileScreen.kt` (Sección Logout)

```kotlin
// Dentro de ProfileScreen, actualizar el botón de logout:

@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = viewModel(
        factory = ProfileViewModelFactory(LocalContext.current)
    ),
    onLogout: () -> Unit,
    onEditProfile: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // ... contenido existente ...

        Spacer(modifier = Modifier.weight(1f))

        // Botón de cerrar sesión
        Button(
            onClick = {
                viewModel.logout {
                    onLogout()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.error
            ),
            enabled = !uiState.isLoading
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onError
                )
            } else {
                Text("Cerrar Sesión")
            }
        }
    }
}
```

### Archivo: `viewmodel/ProfileViewModel.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.repository.AuthRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.UserLocal
import com.example.goldenburgers.model.UserLocalDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class ProfileUiState(
    val nombreCliente: String = "",
    val email: String = "",
    val telefono: String = "",
    val profileImageUri: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authRepository: AuthRepository,
    private val sessionManager: SessionManager,
    private val userLocalDao: UserLocalDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                // Cargar datos de sesión
                val email = sessionManager.loggedInUserEmailFlow.first()
                val clientName = sessionManager.clientNameFlow.first()
                val firebaseUid = sessionManager.firebaseUidFlow.first()

                // Cargar datos locales
                val userLocal = firebaseUid?.let {
                    userLocalDao.getByFirebaseUid(it)
                }

                _uiState.update {
                    it.copy(
                        nombreCliente = clientName ?: "",
                        email = email ?: "",
                        profileImageUri = userLocal?.profileImageUri,
                        isLoading = false
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    /**
     * Cierra sesión y navega a welcome.
     */
    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            authRepository.logout()

            _uiState.update { it.copy(isLoading = false) }
            onComplete()
        }
    }
}
```

---

## PASO 11: IMPLEMENTAR CARGA DE PRODUCTOS DESDE BACKEND

### Archivo: `data/repository/CatalogRepository.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.data.repository

import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.remote.ApiService
import com.example.goldenburgers.data.remote.dto.response.CategoriaResponse
import com.example.goldenburgers.data.remote.dto.response.ProductoResponse
import com.example.goldenburgers.model.FavoriteProduct
import com.example.goldenburgers.model.FavoriteProductDao
import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/**
 * Repository para catálogo de productos.
 */
class CatalogRepository(
    private val sessionManager: SessionManager,
    private val favoriteProductDao: FavoriteProductDao
) {
    private val apiService: ApiService by lazy {
        ApiClient.getApiService(sessionManager)
    }

    /**
     * Resultado sellado para operaciones.
     */
    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // ============================================
    // PRODUCTOS
    // ============================================

    /**
     * Obtiene todos los productos disponibles.
     */
    suspend fun getProductos(): Result<List<ProductoResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProductos()

            if (response.isSuccessful) {
                val productos = response.body() ?: emptyList()
                Result.Success(productos)
            } else {
                Result.Error("Error ${response.code()}: ${response.message()}")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene productos por categoría.
     */
    suspend fun getProductosPorCategoria(
        idCategoria: Long
    ): Result<List<ProductoResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProductosPorCategoria(idCategoria)

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Busca productos por nombre.
     */
    suspend fun buscarProductos(
        query: String
    ): Result<List<ProductoResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.buscarProductos(query)

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    // ============================================
    // CATEGORÍAS
    // ============================================

    /**
     * Obtiene todas las categorías.
     */
    suspend fun getCategorias(): Result<List<CategoriaResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCategorias()

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    // ============================================
    // FAVORITOS (Locales por usuario)
    // ============================================

    /**
     * Obtiene IDs de productos favoritos del usuario actual.
     */
    fun getFavoriteProductIds(): Flow<List<Long>> {
        return sessionManager.firebaseUidFlow.map { uid ->
            if (uid != null) {
                favoriteProductDao.getFavoriteProductIds(uid).first()
            } else {
                emptyList()
            }
        }
    }

    /**
     * Obtiene favoritos como Flow reactivo.
     */
    suspend fun getFavoriteProductIdsFlow(): Flow<List<Long>> {
        val uid = sessionManager.firebaseUidFlow.first()
        return if (uid != null) {
            favoriteProductDao.getFavoriteProductIds(uid)
        } else {
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }

    /**
     * Toggle favorito.
     *
     * @return true si ahora es favorito, false si se quitó
     */
    suspend fun toggleFavorite(productId: Long): Boolean = withContext(Dispatchers.IO) {
        val uid = sessionManager.firebaseUidFlow.first()
            ?: return@withContext false

        favoriteProductDao.toggleFavorite(productId, uid)
    }

    /**
     * Verifica si un producto es favorito.
     */
    suspend fun isFavorite(productId: Long): Boolean = withContext(Dispatchers.IO) {
        val uid = sessionManager.firebaseUidFlow.first()
            ?: return@withContext false

        favoriteProductDao.isFavorite(productId, uid)
    }
}
```

### Actualizar: `viewmodel/CatalogViewModel.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.remote.dto.response.CategoriaResponse
import com.example.goldenburgers.data.remote.dto.response.ProductoResponse
import com.example.goldenburgers.data.repository.CatalogRepository
import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Item del carrito.
 */
data class CartItem(
    val product: ProductoResponse,
    val quantity: Int
)

/**
 * Estado UI del catálogo.
 */
data class CatalogUiState(
    val products: List<ProductoResponse> = emptyList(),
    val categories: List<CategoriaResponse> = emptyList(),
    val favoriteIds: Set<Long> = emptySet(),
    val cartItems: List<CartItem> = emptyList(),
    val selectedCategoryId: Long? = null,
    val searchQuery: String = "",
    val userName: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) {
    // Productos filtrados por categoría
    val filteredProducts: List<ProductoResponse>
        get() = if (selectedCategoryId == null) {
            products
        } else {
            products.filter { it.idCategoria == selectedCategoryId }
        }

    // Productos favoritos
    val favoriteProducts: List<ProductoResponse>
        get() = products.filter { favoriteIds.contains(it.id) }

    // Subtotal del carrito
    val cartSubtotal: Double
        get() = cartItems.sumOf { it.product.precio * it.quantity }

    // Cantidad total de items en carrito
    val cartItemCount: Int
        get() = cartItems.sumOf { it.quantity }
}

/**
 * ViewModel para catálogo, carrito y favoritos.
 */
class CatalogViewModel(
    private val catalogRepository: CatalogRepository,
    private val sessionManager: SessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CatalogUiState())
    val uiState: StateFlow<CatalogUiState> = _uiState.asStateFlow()

    init {
        loadInitialData()
        observeFavorites()
        observeUserName()
    }

    // ============================================
    // CARGA INICIAL
    // ============================================

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Cargar categorías
            when (val result = catalogRepository.getCategorias()) {
                is CatalogRepository.Result.Success -> {
                    _uiState.update { it.copy(categories = result.data) }
                }
                is CatalogRepository.Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }

            // Cargar productos
            when (val result = catalogRepository.getProductos()) {
                is CatalogRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data,
                            isLoading = false
                        )
                    }
                }
                is CatalogRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    /**
     * Refresca productos desde el servidor.
     */
    fun refreshProducts() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = catalogRepository.getProductos()) {
                is CatalogRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            products = result.data,
                            isLoading = false
                        )
                    }
                }
                is CatalogRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    // ============================================
    // FAVORITOS
    // ============================================

    private fun observeFavorites() {
        viewModelScope.launch {
            catalogRepository.getFavoriteProductIdsFlow().collect { ids ->
                _uiState.update { it.copy(favoriteIds = ids.toSet()) }
            }
        }
    }

    /**
     * Toggle favorito de un producto.
     */
    fun toggleFavorite(productId: Long) {
        viewModelScope.launch {
            val newState = catalogRepository.toggleFavorite(productId)

            // Actualizar estado local inmediatamente
            _uiState.update { state ->
                val newFavorites = if (newState) {
                    state.favoriteIds + productId
                } else {
                    state.favoriteIds - productId
                }
                state.copy(favoriteIds = newFavorites)
            }
        }
    }

    /**
     * Verifica si un producto es favorito.
     */
    fun isFavorite(productId: Long): Boolean {
        return _uiState.value.favoriteIds.contains(productId)
    }

    // ============================================
    // CATEGORÍAS
    // ============================================

    /**
     * Selecciona una categoría para filtrar.
     */
    fun selectCategory(categoryId: Long?) {
        _uiState.update { it.copy(selectedCategoryId = categoryId) }
    }

    // ============================================
    // BÚSQUEDA
    // ============================================

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }

        if (query.length >= 2) {
            searchProducts(query)
        } else if (query.isEmpty()) {
            loadInitialData()
        }
    }

    private fun searchProducts(query: String) {
        viewModelScope.launch {
            when (val result = catalogRepository.buscarProductos(query)) {
                is CatalogRepository.Result.Success -> {
                    _uiState.update { it.copy(products = result.data) }
                }
                is CatalogRepository.Result.Error -> {
                    // Mantener productos actuales
                }
            }
        }
    }

    // ============================================
    // CARRITO
    // ============================================

    /**
     * Agrega producto al carrito.
     */
    fun addToCart(product: ProductoResponse) {
        _uiState.update { state ->
            val existingItem = state.cartItems.find { it.product.id == product.id }

            val newCartItems = if (existingItem != null) {
                // Incrementar cantidad
                state.cartItems.map {
                    if (it.product.id == product.id) {
                        it.copy(quantity = it.quantity + 1)
                    } else {
                        it
                    }
                }
            } else {
                // Agregar nuevo item
                state.cartItems + CartItem(product, 1)
            }

            state.copy(cartItems = newCartItems)
        }
    }

    /**
     * Elimina producto del carrito.
     */
    fun removeFromCart(productId: Long) {
        _uiState.update { state ->
            state.copy(
                cartItems = state.cartItems.filter { it.product.id != productId }
            )
        }
    }

    /**
     * Incrementa cantidad de un producto.
     */
    fun increaseQuantity(productId: Long) {
        _uiState.update { state ->
            state.copy(
                cartItems = state.cartItems.map {
                    if (it.product.id == productId) {
                        it.copy(quantity = it.quantity + 1)
                    } else {
                        it
                    }
                }
            )
        }
    }

    /**
     * Decrementa cantidad de un producto.
     */
    fun decreaseQuantity(productId: Long) {
        _uiState.update { state ->
            val newItems = state.cartItems.mapNotNull {
                if (it.product.id == productId) {
                    if (it.quantity > 1) {
                        it.copy(quantity = it.quantity - 1)
                    } else {
                        null // Eliminar si llega a 0
                    }
                } else {
                    it
                }
            }
            state.copy(cartItems = newItems)
        }
    }

    /**
     * Limpia el carrito.
     */
    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    // ============================================
    // USUARIO
    // ============================================

    private fun observeUserName() {
        viewModelScope.launch {
            sessionManager.clientNameFlow.collect { name ->
                _uiState.update { it.copy(userName = name) }
            }
        }
    }

    // ============================================
    // ERRORES
    // ============================================

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

---

## PASO 12: IMPLEMENTAR CATEGORÍAS

### Actualizar: `view/HomeScreen.kt`

```kotlin
@Composable
fun HomeScreen(
    viewModel: CatalogViewModel,
    onNavigateToCart: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Barra de búsqueda
        SearchBar(
            query = uiState.searchQuery,
            onQueryChange = viewModel::onSearchQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )

        // Chips de categorías
        if (uiState.categories.isNotEmpty()) {
            CategoriesRow(
                categories = uiState.categories,
                selectedCategoryId = uiState.selectedCategoryId,
                onCategorySelected = viewModel::selectCategory
            )
        }

        // Loading indicator
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }

        // Error message
        uiState.errorMessage?.let { error ->
            ErrorMessage(
                message = error,
                onRetry = viewModel::refreshProducts,
                onDismiss = viewModel::clearError
            )
        }

        // Grid de productos
        if (!uiState.isLoading && uiState.errorMessage == null) {
            ProductsGrid(
                products = uiState.filteredProducts,
                favoriteIds = uiState.favoriteIds,
                onProductClick = { /* Detalle */ },
                onAddToCart = viewModel::addToCart,
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
    }
}

@Composable
fun CategoriesRow(
    categories: List<CategoriaResponse>,
    selectedCategoryId: Long?,
    onCategorySelected: (Long?) -> Unit
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Chip "Todos"
        item {
            FilterChip(
                selected = selectedCategoryId == null,
                onClick = { onCategorySelected(null) },
                label = { Text("Todos") }
            )
        }

        // Chips de categorías
        items(categories) { category ->
            FilterChip(
                selected = selectedCategoryId == category.id,
                onClick = { onCategorySelected(category.id) },
                label = { Text(category.nombre) }
            )
        }
    }
}

@Composable
fun ProductsGrid(
    products: List<ProductoResponse>,
    favoriteIds: Set<Long>,
    onProductClick: (ProductoResponse) -> Unit,
    onAddToCart: (ProductoResponse) -> Unit,
    onToggleFavorite: (Long) -> Unit
) {
    if (products.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No hay productos disponibles",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                ProductCard(
                    product = product,
                    isFavorite = favoriteIds.contains(product.id),
                    onAddToCart = { onAddToCart(product) },
                    onToggleFavorite = { onToggleFavorite(product.id) },
                    onClick = { onProductClick(product) }
                )
            }
        }
    }
}

@Composable
fun ProductCard(
    product: ProductoResponse,
    isFavorite: Boolean,
    onAddToCart: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Imagen del producto (desde URL)
            Box {
                AsyncImage(
                    model = product.imagen,
                    contentDescription = product.nombre,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentScale = ContentScale.Crop,
                    placeholder = painterResource(R.drawable.placeholder_burger),
                    error = painterResource(R.drawable.placeholder_burger)
                )

                // Botón favorito
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = if (isFavorite) {
                            Icons.Filled.Favorite
                        } else {
                            Icons.Outlined.FavoriteBorder
                        },
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.Gray
                    )
                }
            }

            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = product.nombre,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = product.descripcion ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$${product.precio.toInt()}",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    IconButton(
                        onClick = onAddToCart,
                        colors = IconButtonDefaults.iconButtonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.AddShoppingCart,
                            contentDescription = "Agregar al carrito",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ErrorMessage(
    message: String,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }

            Row {
                TextButton(onClick = onRetry) {
                    Text("Reintentar")
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Cerrar")
                }
            }
        }
    }
}
```

---

## PASO 13: IMPLEMENTAR FAVORITOS LOCALES POR USUARIO

### Actualizar: `view/FavoritesScreen.kt`

```kotlin
@Composable
fun FavoritesScreen(
    viewModel: CatalogViewModel
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        // Título
        Text(
            text = "Mis Favoritos",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(16.dp)
        )

        if (uiState.favoriteProducts.isEmpty()) {
            // Estado vacío
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No tienes favoritos aún",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = "Agrega productos tocando el corazón",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // Grid de favoritos
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(uiState.favoriteProducts) { product ->
                    ProductCard(
                        product = product,
                        isFavorite = true,
                        onAddToCart = { viewModel.addToCart(product) },
                        onToggleFavorite = { viewModel.toggleFavorite(product.id) },
                        onClick = { /* Detalle */ }
                    )
                }
            }
        }
    }
}
```

---

## PASO 14: IMPLEMENTAR EDICIÓN DE PERFIL

### Actualizar: `viewmodel/EditProfileViewModel.kt`

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.remote.dto.request.ActualizarPerfilRequest
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.UserLocal
import com.example.goldenburgers.model.UserLocalDao
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditProfileUiState(
    val nombreCliente: String = "",
    val email: String = "",
    val telefono: String = "",
    val profileImageUri: String? = null,
    val street: String = "",
    val number: String = "",
    val commune: String = "",
    val city: String = "",
    val region: String = "",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class EditProfileViewModel(
    private val sessionManager: SessionManager,
    private val userLocalDao: UserLocalDao
) : ViewModel() {

    private val apiService by lazy {
        ApiClient.getApiService(sessionManager)
    }

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var firebaseUid: String? = null

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                firebaseUid = sessionManager.firebaseUidFlow.first()
                val email = sessionManager.loggedInUserEmailFlow.first()
                val clientId = sessionManager.clientIdFlow.first()

                // Cargar datos del backend
                if (firebaseUid != null) {
                    val response = apiService.getClientePorUsuario(firebaseUid!!)
                    if (response.isSuccessful) {
                        val cliente = response.body()
                        _uiState.update {
                            it.copy(
                                nombreCliente = cliente?.nombreCliente ?: "",
                                email = cliente?.usuario?.email ?: email ?: "",
                                telefono = cliente?.telefonoCliente ?: ""
                            )
                        }
                    }
                }

                // Cargar datos locales
                val userLocal = firebaseUid?.let { userLocalDao.getByFirebaseUid(it) }
                if (userLocal != null) {
                    _uiState.update {
                        it.copy(
                            profileImageUri = userLocal.profileImageUri,
                            street = userLocal.street ?: "",
                            number = userLocal.number ?: "",
                            commune = userLocal.commune ?: "",
                            city = userLocal.city ?: "",
                            region = userLocal.region ?: ""
                        )
                    }
                }

                _uiState.update { it.copy(isLoading = false) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message
                    )
                }
            }
        }
    }

    // Actualizadores de campos
    fun onNombreChange(nombre: String) {
        _uiState.update { it.copy(nombreCliente = nombre) }
    }

    fun onTelefonoChange(telefono: String) {
        val filtered = telefono.filter { it.isDigit() }.take(9)
        _uiState.update { it.copy(telefono = filtered) }
    }

    fun onStreetChange(street: String) {
        _uiState.update { it.copy(street = street) }
    }

    fun onNumberChange(number: String) {
        _uiState.update { it.copy(number = number) }
    }

    fun onCommuneChange(commune: String) {
        _uiState.update { it.copy(commune = commune) }
    }

    fun onCityChange(city: String) {
        _uiState.update { it.copy(city = city) }
    }

    fun onRegionChange(region: String) {
        _uiState.update { it.copy(region = region) }
    }

    fun onProfileImageChange(uri: String?) {
        _uiState.update { it.copy(profileImageUri = uri) }

        // Guardar inmediatamente en Room
        viewModelScope.launch {
            firebaseUid?.let { uid ->
                userLocalDao.updateProfileImage(uid, uri)
            }
        }
    }

    /**
     * Guarda los cambios en backend y Room.
     */
    fun saveChanges(onSuccess: () -> Unit) {
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }

            try {
                // 1. Actualizar en backend
                val request = ActualizarPerfilRequest(
                    nombreCliente = state.nombreCliente,
                    telefonoCliente = state.telefono.ifBlank { null }
                )

                val response = apiService.actualizarPerfil(request)

                if (!response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Error al guardar: ${response.code()}"
                        )
                    }
                    return@launch
                }

                // 2. Actualizar datos locales en Room
                firebaseUid?.let { uid ->
                    userLocalDao.updateAddress(
                        firebaseUid = uid,
                        street = state.street,
                        number = state.number,
                        commune = state.commune,
                        city = state.city,
                        region = state.region
                    )
                }

                // 3. Actualizar nombre en sesión
                sessionManager.saveSession(
                    email = state.email,
                    firebaseUid = firebaseUid ?: "",
                    jwtToken = sessionManager.jwtTokenFlow.first() ?: "",
                    clientId = sessionManager.clientIdFlow.first() ?: 0L,
                    clientName = state.nombreCliente
                )

                _uiState.update {
                    it.copy(
                        isSaving = false,
                        successMessage = "Perfil actualizado correctamente"
                    )
                }

                onSuccess()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = e.message ?: "Error al guardar"
                    )
                }
            }
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }
}
```

---

## PASO 15: IMPLEMENTAR GESTIÓN DE DIRECCIONES

### Archivo: `data/repository/AddressRepository.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.data.repository

import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.remote.ApiService
import com.example.goldenburgers.data.remote.dto.request.ActualizarDireccionRequest
import com.example.goldenburgers.data.remote.dto.request.CrearDireccionRequest
import com.example.goldenburgers.data.remote.dto.response.CiudadResponse
import com.example.goldenburgers.data.remote.dto.response.DireccionResponse
import com.example.goldenburgers.model.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository para gestión de direcciones.
 */
class AddressRepository(
    private val sessionManager: SessionManager
) {
    private val apiService: ApiService by lazy {
        ApiClient.getApiService(sessionManager)
    }

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    /**
     * Obtiene las direcciones del cliente actual.
     */
    suspend fun getDirecciones(): Result<List<DireccionResponse>> = withContext(Dispatchers.IO) {
        try {
            val clientId = sessionManager.clientIdFlow.first()
                ?: return@withContext Result.Error("No hay sesión activa")

            val response = apiService.getDirecciones(clientId)

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene lista de ciudades disponibles.
     */
    suspend fun getCiudades(): Result<List<CiudadResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getCiudades()

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Agrega una nueva dirección.
     *
     * @param street Calle
     * @param number Número
     * @param commune Comuna
     * @param city Ciudad
     * @param region Región
     * @param alias Alias ("Casa", "Trabajo", etc.)
     */
    suspend fun agregarDireccion(
        street: String,
        number: String,
        commune: String,
        city: String,
        region: String,
        alias: String? = null
    ): Result<DireccionResponse> = withContext(Dispatchers.IO) {
        try {
            val clientId = sessionManager.clientIdFlow.first()
                ?: return@withContext Result.Error("No hay sesión activa")

            // Concatenar dirección
            val direccionConcatenada = "$street $number, $commune, $city, $region"

            // Buscar ID de ciudad
            val ciudadResponse = apiService.getCiudadPorNombre(city)
            val idCiudad = if (ciudadResponse.isSuccessful) {
                ciudadResponse.body()?.idCiudad ?: 1L
            } else {
                1L
            }

            val request = CrearDireccionRequest(
                idCliente = clientId,
                idCiudad = idCiudad,
                direccion = direccionConcatenada,
                alias = alias
            )

            val response = apiService.agregarDireccion(request)

            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Actualiza una dirección existente.
     */
    suspend fun actualizarDireccion(
        idDireccion: Long,
        street: String,
        number: String,
        commune: String,
        city: String,
        region: String,
        alias: String? = null
    ): Result<DireccionResponse> = withContext(Dispatchers.IO) {
        try {
            val direccionConcatenada = "$street $number, $commune, $city, $region"

            val ciudadResponse = apiService.getCiudadPorNombre(city)
            val idCiudad = if (ciudadResponse.isSuccessful) {
                ciudadResponse.body()?.idCiudad ?: 1L
            } else {
                1L
            }

            val request = ActualizarDireccionRequest(
                idCiudad = idCiudad,
                direccion = direccionConcatenada,
                alias = alias
            )

            val response = apiService.actualizarDireccion(idDireccion, request)

            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Elimina una dirección.
     */
    suspend fun eliminarDireccion(idDireccion: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.eliminarDireccion(idDireccion)

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }
}
```

---

## PASO 16: MANTENER FOTO DE PERFIL LOCAL

La foto de perfil ya está configurada para guardarse localmente en los pasos anteriores.

### Resumen de persistencia de foto:

1. **Durante registro** (RegisterStep3Screen):
   - Se captura URI de cámara/galería
   - Se guarda en `UserLocal.profileImageUri`

2. **Durante edición** (EditProfileScreen):
   - Se puede cambiar la foto
   - Se actualiza en Room inmediatamente

3. **Al mostrar** (ProfileScreen, EditProfileScreen):
   - Se carga desde `UserLocal` usando el `firebaseUid`
   - Se muestra con Coil desde URI local

### Código para mostrar foto de perfil:

```kotlin
@Composable
fun ProfileImage(
    imageUri: String?,
    modifier: Modifier = Modifier
) {
    if (imageUri != null) {
        AsyncImage(
            model = imageUri,
            contentDescription = "Foto de perfil",
            modifier = modifier
                .size(100.dp)
                .clip(CircleShape)
                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
            contentScale = ContentScale.Crop
        )
    } else {
        // Placeholder
        Box(
            modifier = modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "Sin foto",
                modifier = Modifier.size(50.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

---

## RESUMEN PARTE 2

### Archivos creados/modificados:

| Archivo | Acción | Descripción |
|---------|--------|-------------|
| `AuthRepository.kt` | Crear | Registro/Login con Firebase + Backend |
| `RegisterViewModel.kt` | Modificar | Usar AuthRepository |
| `RegisterViewModelFactory.kt` | Crear/Modificar | Factory con dependencias |
| `LoginViewModel.kt` | Modificar | Usar AuthRepository |
| `LoginViewModelFactory.kt` | Crear/Modificar | Factory con dependencias |
| `CatalogRepository.kt` | Crear | Productos y favoritos |
| `CatalogViewModel.kt` | Modificar | Usar CatalogRepository |
| `ProfileViewModel.kt` | Crear/Modificar | Logout y datos de perfil |
| `EditProfileViewModel.kt` | Modificar | Edición con backend + local |
| `AddressRepository.kt` | Crear | CRUD de direcciones |
| `HomeScreen.kt` | Modificar | Categorías y productos de backend |
| `FavoritesScreen.kt` | Modificar | Favoritos locales por usuario |

### Próxima parte:
Continúa en **INTEGRACION-PARTE3-PEDIDOS.md** para implementar pedidos, carrito y pago con Mercado Pago.

---

**Anterior:** [PARTE 1 - Configuración](./INTEGRACION-PARTE1-CONFIGURACION.md)

**Siguiente:** [PARTE 3 - Pedidos y Pagos](./INTEGRACION-PARTE3-PEDIDOS.md)
