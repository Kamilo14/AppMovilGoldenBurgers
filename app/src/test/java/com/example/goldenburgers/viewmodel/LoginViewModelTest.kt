package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.model.SessionManager
import com.google.firebase.auth.FirebaseUser
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class LoginViewModelTest : ShouldSpec() {

    private lateinit var authRepository: AuthRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: LoginViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            authRepository = mockk()
            clienteRepository = mockk()
            sessionManager = mockk(relaxed = true)
            viewModel = LoginViewModel(authRepository, clienteRepository, sessionManager)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Funcionalidad de Login") {
            should("Debería llamar a los repositorios y navegar al éxito cuando el login es correcto") {
                // Arrange
                val usuarioFalso = mockk<FirebaseUser> {
                    every { uid } returns "uid-falso"
                    every { email } returns "test@test.com"
                }
                val fakeFirebaseToken = "fake-firebase-token"
                // [CORRECCIÓN CLAVE] exchangeToken devuelve un Result<String>, no un objeto.
                val fakeInternalToken = "fake-internal-jwt"

                // Simulamos la cadena completa de llamadas exitosas
                coEvery { authRepository.login(any(), any()) } returns Result.success(usuarioFalso)
                coEvery { authRepository.getFirebaseToken() } returns fakeFirebaseToken
                coEvery { authRepository.exchangeToken(fakeFirebaseToken) } returns Result.success(fakeInternalToken)
                coEvery { clienteRepository.obtenerClientePorFirebaseUid(any()) } returns Result.success(mockk())

                var fueExitoso = false

                // Act
                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("123456")
                viewModel.login(
                    onSuccess = { fueExitoso = true },
                    onError = {}
                )

                // Assert
                fueExitoso shouldBe true

                // Verificamos que toda la cadena de funciones fue llamada en orden
                coVerifyOrder {
                    authRepository.login("test@test.com", "123456")
                    authRepository.getFirebaseToken()
                    authRepository.exchangeToken(fakeFirebaseToken)
                    sessionManager.saveUserSession("test@test.com", fakeInternalToken)
                    clienteRepository.obtenerClientePorFirebaseUid("uid-falso")
                }
            }

            should("debería mostrar un error cuando el login falla") {
                // Arrange
                val mensajeError = "Credenciales inválidas"
                coEvery { authRepository.login(any(), any()) } returns Result.failure(Exception(mensajeError))

                var errorRecibido: String? = null

                // Act
                viewModel.onEmailChange("test@test.com")
                viewModel.onPasswordChange("clave-incorrecta")
                viewModel.login(
                    onSuccess = {},
                    onError = { errorRecibido = it }
                )

                // Assert
                errorRecibido shouldBe mensajeError
                coVerify(exactly = 0) { sessionManager.saveUserSession(any(), any()) }
            }
        }
    }
}