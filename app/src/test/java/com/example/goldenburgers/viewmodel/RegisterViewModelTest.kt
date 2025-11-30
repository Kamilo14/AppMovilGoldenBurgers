package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.RegistrationResult
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class RegisterViewModelTest : ShouldSpec() {

    private lateinit var authRepository: AuthRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: RegisterViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            authRepository = mockk(relaxed = true)
            clienteRepository = mockk(relaxed = true)
            sessionManager = mockk(relaxed = true)
            viewModel = RegisterViewModel(authRepository, clienteRepository, sessionManager)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Funcionalidad de Registro") {
            should("registrar y guardar sesión cuando los datos son válidos (con dirección)") {
                // Arrange
                val fakeRol = Rol(3, "Cliente")
                val fakeUsuario = Usuario("uid-falso", "test@test.com", fakeRol, "2025-01-01")
                val fakeCliente = Cliente(1L, fakeUsuario, "Test User", "123456789", emptyList())
                val fakeResult = RegistrationResult(fakeCliente, "fake-internal-token")
                val fakeDireccion = mockk<DireccionCliente>() // [CORRECCIÓN CLAVE] Creamos un mock específico

                coEvery { authRepository.registerUser(any(), any(), any(), any()) } returns Result.success(fakeResult)
                coEvery { clienteRepository.crearDireccion(any(), any(), any(), any()) } returns Result.success(fakeDireccion) // Lo devolvemos

                var fueExitoso = false
                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("123456")
                viewModel.onFullNameChange("Test User")
                viewModel.onDireccionChange("Direccion Valida 123")
                viewModel.onCiudadChange(1L)
                
                // Act
                viewModel.onRegisterClicked(onSuccess = { fueExitoso = true }, onError = {})

                // Assert
                fueExitoso shouldBe true
                coVerify(exactly = 1) { clienteRepository.crearDireccion(any(), any(), any(), any()) }
            }

            should("registrar y guardar sesión cuando los datos son válidos (SIN dirección)") {
                val fakeCliente = Cliente(1L, Usuario("uid", "e@e.com", Rol(1, "R"), "f"), "Test User", null, emptyList())
                val fakeResult = RegistrationResult(fakeCliente, "fake-token")
                coEvery { authRepository.registerUser(any(), any(), any(), any()) } returns Result.success(fakeResult)
                var fueExitoso = false

                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("123456")
                viewModel.onFullNameChange("Test User")
                
                viewModel.onRegisterClicked(onSuccess = { fueExitoso = true }, onError = {})

                fueExitoso shouldBe true
                coVerify(exactly = 0) { clienteRepository.crearDireccion(any(), any(), any(), any()) }
            }

            should("mostrar un error si el registro falla") {
                val mensajeError = "ERROR_EMAIL_ALREADY_IN_USE"
                coEvery { authRepository.registerUser(any(), any(), any(), any()) } returns Result.failure(Exception(mensajeError))
                var errorRecibido: String? = null
                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("123456")
                viewModel.onFullNameChange("Test User")
                viewModel.onDireccionChange("Direccion")
                viewModel.onCiudadChange(1L)

                viewModel.onRegisterClicked(onSuccess = {}, onError = { errorRecibido = it })

                errorRecibido shouldBe mensajeError
            }

            should("mostrar un error si la creación de la dirección falla") {
                val fakeCliente = Cliente(1L, Usuario("uid", "e@e.com", Rol(1, "R"), "f"), "Test User", null, emptyList())
                val fakeResult = RegistrationResult(fakeCliente, "fake-token")
                coEvery { authRepository.registerUser(any(), any(), any(), any()) } returns Result.success(fakeResult)
                coEvery { clienteRepository.crearDireccion(any(), any(), any(), any()) } returns Result.failure(Exception("Error de BD"))
                var errorRecibido: String? = null
                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("123456")
                viewModel.onFullNameChange("Test User")
                viewModel.onDireccionChange("Direccion Falsa 123")
                viewModel.onCiudadChange(1L)

                viewModel.onRegisterClicked(onSuccess = {}, onError = { errorRecibido = it })

                errorRecibido?.contains("Error de BD") shouldBe true
            }

            should("mostrar un error si el formulario no es válido") {
                var errorRecibido: String? = null
                viewModel.onEmailChange("test@test.com") 

                viewModel.onRegisterClicked(onSuccess = {}, onError = { errorRecibido = it })

                errorRecibido shouldBe "El formulario contiene errores o datos incompletos."
                coVerify(exactly = 0) { authRepository.registerUser(any(), any(), any(), any()) }
            }
        }

        context("Validación de campos") {
            should("quitar el error de email cuando se corrige") {
                viewModel.onEmailChange("email-invalido")
                viewModel.uiState.value.emailError shouldNotBe null
                viewModel.onEmailChange("email.valido@test.com")
                viewModel.uiState.value.emailError shouldBe null
            }

            should("quitar el error de contraseña cuando se corrige") {
                viewModel.onPasswordChange("123")
                viewModel.uiState.value.passwordError shouldNotBe null
                viewModel.onPasswordChange("123456")
                viewModel.uiState.value.passwordError shouldBe null
            }

            should("quitar el error de nombre cuando se corrige") {
                viewModel.onFullNameChange("Ana")
                viewModel.uiState.value.fullNameError shouldNotBe null
                viewModel.onFullNameChange("Nombre Largo Valido")
                viewModel.uiState.value.fullNameError shouldBe null
            }

            should("permitir un número de teléfono vacío") {
                viewModel.onPhoneNumberChange("")
                viewModel.uiState.value.phoneNumberError shouldBe null
            }

            should("onAliasChange debería actualizar el estado") {
                viewModel.onAliasChange("Mi Casa")
                viewModel.uiState.value.alias shouldBe "Mi Casa"
            }

            should("onProfileImageChange debería actualizar el estado") {
                viewModel.onProfileImageChange("nueva_uri")
                viewModel.uiState.value.profileImageUri shouldBe "nueva_uri"
            }

            should("onFetchingLocationChange debería actualizar el estado") {
                viewModel.onFetchingLocationChange(true)
                viewModel.uiState.value.isFetchingLocation shouldBe true
            }
        }
    }
}