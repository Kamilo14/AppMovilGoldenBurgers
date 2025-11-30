package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.google.firebase.auth.FirebaseUser
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class EditProfileViewModelTest : ShouldSpec() {

    private lateinit var authRepository: AuthRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var sessionManager: SessionManager
    private lateinit var viewModel: EditProfileViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            authRepository = mockk()
            clienteRepository = mockk()
            sessionManager = mockk(relaxed = true)
            viewModel = EditProfileViewModel(authRepository, clienteRepository, sessionManager)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Cargar Perfil de Usuario") {
            should("debería cargar los datos del cliente y actualizar el estado con éxito") {
                val fakeFirebaseUser = mockk<FirebaseUser> { every { uid } returns "uid-falso" }
                val fakeCliente = Cliente(1L, Usuario("uid", "e@e.com", Rol(1, "R"), "f"), "Test User", "123", emptyList())
                coEvery { authRepository.getCurrentUser() } returns fakeFirebaseUser
                coEvery { clienteRepository.obtenerClientePorFirebaseUid(any()) } returns Result.success(fakeCliente)

                viewModel.loadCurrentUser()

                val state = viewModel.uiState.value
                state.nombreCliente shouldBe "Test User"
                state.isLoading shouldBe false
            }
        }

        context("Guardar Cambios del Perfil") {
            should("debería llamar al repositorio para actualizar y ejecutar onSuccess") {
                val fakeFirebaseUser = mockk<FirebaseUser> { every { uid } returns "uid-falso" }
                val initialCliente = Cliente(1L, Usuario("uid", "e@e.com", Rol(1, "R"), "f"), "Test User", "123", emptyList())
                coEvery { authRepository.getCurrentUser() } returns fakeFirebaseUser
                coEvery { clienteRepository.obtenerClientePorFirebaseUid(any()) } returns Result.success(initialCliente)
                viewModel.loadCurrentUser()

                val clienteActualizado = initialCliente.copy(nombreCliente = "Nuevo Nombre")
                coEvery { clienteRepository.actualizarPerfil(any(), any(), any()) } returns Result.success(clienteActualizado)
                var fueExitoso = false

                viewModel.onNombreClienteChange("Nuevo Nombre")
                viewModel.saveChanges(onSuccess = { fueExitoso = true }, onError = {})

                fueExitoso shouldBe true
            }
        }

        context("Caminos de Error y Casos de Borde") {
            should("loadCurrentUser debería parar de cargar si no hay usuario de Firebase") {
                coEvery { authRepository.getCurrentUser() } returns null
                viewModel.loadCurrentUser()
                viewModel.uiState.value.isLoading shouldBe false
            }

            should("loadCurrentUser debería parar de cargar si el repositorio de cliente falla") {
                // [CORRECCIÓN CLAVE 1] El mock de FirebaseUser debe tener un uid
                val fakeFirebaseUser = mockk<FirebaseUser> { every { uid } returns "uid-falso" }
                coEvery { authRepository.getCurrentUser() } returns fakeFirebaseUser
                coEvery { clienteRepository.obtenerClientePorFirebaseUid(any()) } returns Result.failure(Exception())
                
                viewModel.loadCurrentUser()
                
                viewModel.uiState.value.isLoading shouldBe false
            }

            should("saveChanges debería invocar onError si no hay un cliente cargado") {
                var errorRecibido: String? = null
                viewModel.saveChanges(onSuccess = {}, onError = { errorRecibido = it })
                errorRecibido shouldBe "No se pudo encontrar la información del cliente."
            }

            should("saveChanges debería invocar onError si el repositorio falla al actualizar") {
                // [CORRECCIÓN CLAVE 2] Se configura el mock de FirebaseUser y se carga el estado inicial
                val fakeFirebaseUser = mockk<FirebaseUser> { every { uid } returns "uid-falso" }
                val initialCliente = Cliente(1L, Usuario("uid", "e@e.com", Rol(1, "R"), "f"), "Test User", null, emptyList())
                coEvery { authRepository.getCurrentUser() } returns fakeFirebaseUser
                coEvery { clienteRepository.obtenerClientePorFirebaseUid(any()) } returns Result.success(initialCliente)
                viewModel.loadCurrentUser()

                val mensajeError = "Error de red"
                coEvery { clienteRepository.actualizarPerfil(any(), any(), any()) } returns Result.failure(Exception(mensajeError))
                var errorRecibido: String? = null

                viewModel.saveChanges(onSuccess = {}, onError = { errorRecibido = it })

                errorRecibido shouldBe mensajeError
            }

            should("onProfileImageChange debería actualizar el estado") {
                viewModel.onProfileImageChange("nueva_uri")
                viewModel.uiState.value.profileImageUri shouldBe "nueva_uri"
            }
        }
    }
}