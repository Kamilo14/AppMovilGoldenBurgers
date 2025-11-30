package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.RoutingRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class AddressViewModelTest : ShouldSpec() {

    private lateinit var clienteRepository: ClienteRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var routingRepository: RoutingRepository
    private lateinit var viewModel: AddressViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            clienteRepository = mockk(relaxed = true)
            authRepository = mockk(relaxed = true)
            routingRepository = mockk(relaxed = true)
            viewModel = AddressViewModel(clienteRepository, authRepository, routingRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Carga de Direcciones") {
            should("debería cargar las direcciones desde el cliente actual") {
                val fakeAddress1 = DireccionCliente(1L, 1L, Ciudad(1L, "Viña"), "Calle 1", "Casa")
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", listOf(fakeAddress1))
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                coEvery { authRepository.getCurrentUser() } returns mockk<FirebaseUser>()

                viewModel.loadAddresses()

                viewModel.uiState.value.addresses.size shouldBe 1
            }
        }

        context("Cálculo de Tiempo de Entrega") {
            should("actualizar el estado con el tiempo estimado") {
                val fakeAddress = DireccionCliente(1L, 1L, Ciudad(1L, "Viña del Mar"), "Calle Falsa 123", "Casa")
                coEvery { routingRepository.getEstimatedDeliveryTime(any()) } returns 25

                viewModel.calculateDeliveryTime(fakeAddress)

                viewModel.uiState.value.estimatedDeliveryTime shouldBe 25
            }

            should("resetDeliveryTime debería poner el tiempo estimado a nulo") {
                // Arrange
                // [CORRECCIÓN CLAVE] Se crea un objeto real para no causar el MockKException
                val fakeAddress = DireccionCliente(1L, 1L, Ciudad(1L, "Viña"), "Calle 1", "Casa")
                coEvery { routingRepository.getEstimatedDeliveryTime(any()) } returns 30
                viewModel.calculateDeliveryTime(fakeAddress)
                viewModel.uiState.value.estimatedDeliveryTime shouldNotBe null

                // Act
                viewModel.resetDeliveryTime()

                // Assert
                viewModel.uiState.value.estimatedDeliveryTime shouldBe null
            }
        }

        context("Eliminar Dirección") {
            should("llamar al repositorio para eliminar y actualizar la lista en caso de éxito") {
                val fakeAddress = DireccionCliente(123L, 1L, Ciudad(1L, "Viña"), "Calle 1", "Casa")
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", listOf(fakeAddress))
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                coEvery { clienteRepository.eliminarDireccion(123L) } returns Result.success(Unit)

                viewModel.deleteAddress(123L)

                coVerify { clienteRepository.eliminarDireccion(123L) }
                viewModel.uiState.value.deletingAddressId shouldBe null
            }

            should("actualizar el estado con un error si la eliminación falla") {
                val errorMessage = "Error de base de datos"
                coEvery { clienteRepository.eliminarDireccion(any()) } returns Result.failure(Exception(errorMessage))

                viewModel.deleteAddress(123L)

                val state = viewModel.uiState.value
                state.deletingAddressId shouldBe null
                state.error?.contains(errorMessage) shouldBe true
            }
        }
    }
}