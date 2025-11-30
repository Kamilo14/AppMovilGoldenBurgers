package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.data.Ciudad
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class EditAddressViewModelTest : ShouldSpec() {

    private lateinit var clienteRepository: ClienteRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: EditAddressViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            clienteRepository = mockk(relaxed = true)
            authRepository = mockk(relaxed = true)
            viewModel = EditAddressViewModel(clienteRepository, authRepository)
            viewModel.loadInitialData()
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Cargar Dirección") {
            should("debería rellenar el estado con los datos de una dirección existente") {
                // Arrange
                val fakeCiudad = Ciudad(1L, "Viña del Mar")
                // [CORRECCIÓN CLAVE 1] Se usa el constructor correcto con los parámetros en orden
                val fakeAddress = DireccionCliente(10L, 1L, fakeCiudad, "Calle Falsa 123", "Casa")
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", listOf(fakeAddress))
                
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)

                // Act
                viewModel.loadAddress(10L)

                // Assert
                val state = viewModel.uiState.value
                state.alias shouldBe "Casa"
                state.direccion shouldBe "Calle Falsa 123"
                state.ciudadId shouldBe 1L
                state.isNewAddress shouldBe false
            }
        }

        context("Guardar Dirección") {
            should("debería llamar a 'actualizarDireccion' cuando se guarda una dirección existente") {
                // Arrange
                // [CORRECCIÓN CLAVE 2] Se simula la carga de datos para poner el ViewModel en un estado inicial válido
                val fakeCiudad = Ciudad(1L, "Viña del Mar")
                val fakeAddress = DireccionCliente(10L, 1L, fakeCiudad, "Calle Vieja 123", "Casa")
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", listOf(fakeAddress))
                
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                viewModel.loadAddress(10L) // Carga la dirección, estableciendo isNewAddress = false

                coEvery { clienteRepository.actualizarDireccion(any(), any(), any(), any()) } returns Result.success(mockk())

                // Act
                viewModel.onDireccionChange("Calle Nueva 456")
                viewModel.onCiudadSelected(2L)
                viewModel.saveAddress(10L)

                // Assert
                coVerify { clienteRepository.actualizarDireccion(10L, 2L, "Calle Nueva 456", "Casa") }
            }

            should("debería llamar a 'crearDireccion' cuando se guarda una dirección nueva") {
                // Arrange
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", emptyList())
                
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                viewModel.loadAddress(-1L) // Carga el modo "nueva dirección"

                coEvery { clienteRepository.crearDireccion(any(), any(), any(), any()) } returns Result.success(mockk())

                // Act
                viewModel.onDireccionChange("Avenida Inventada 789")
                viewModel.onCiudadSelected(3L)
                viewModel.onAliasChange("Depto")
                viewModel.saveAddress(-1L)

                // Assert
                coVerify { clienteRepository.crearDireccion(1L, 3L, "Avenida Inventada 789", "Depto") }
            }
        }
    }
}