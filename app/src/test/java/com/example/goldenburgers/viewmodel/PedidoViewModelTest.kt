package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.model.data.Rol
import com.example.goldenburgers.model.data.Usuario
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.PedidoRepository
import com.example.goldenburgers.view.DeliveryOption
import com.example.goldenburgers.view.PaymentOption
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
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
class PedidoViewModelTest : ShouldSpec() {

    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var clienteRepository: ClienteRepository
    private lateinit var authRepository: AuthRepository
    private lateinit var viewModel: PedidoViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            pedidoRepository = mockk()
            clienteRepository = mockk(relaxed = true)
            authRepository = mockk(relaxed = true)
            viewModel = PedidoViewModel(pedidoRepository, clienteRepository, authRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Creación de Pedido") {
            should("debería llamar al repositorio con los datos correctos y actualizar el estado al crear un pedido") {
                // Arrange: Preparamos todos los objetos falsos necesarios
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", emptyList())
                val fakeProduct = Producto(10L, 1L, "Burger Test", "desc", 5000.0, null, 1, "cat")
                val fakeCartItems = listOf(CartItem(fakeProduct, 2))
                val fakeDireccion = DireccionCliente(20L, 1L, mockk(), "Calle Falsa 123", "Casa")
                val fakeDelivery = DeliveryOption(1, "Delivery", "desc", 1000.0)
                val fakePayment = PaymentOption(2, "Efectivo")

                val fakePedidoCreado = mockk<PedidoDTO>()

                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                coEvery { pedidoRepository.crearPedidoCompleto(any()) } returns Result.success(fakePedidoCreado)

                // Act: Llamamos a la función a probar
                viewModel.crearPedido(
                    items = fakeCartItems,
                    subtotal = 10000.0,
                    direccion = fakeDireccion,
                    tipoEntrega = fakeDelivery,
                    metodoPago = fakePayment
                )

                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.ultimoPedidoCreado shouldBe fakePedidoCreado

                coVerify(exactly = 1) { pedidoRepository.crearPedidoCompleto(match {
                    it.idCliente == 1L &&
                    it.idDireccionEntrega == 20L &&
                    it.montoTotal == 11000.0 &&
                    it.detalles.first().idProducto == 10L &&
                    it.detalles.first().cantidad == 2
                }) }
            }
        }

        context("Historial de Pedidos") {
            should("debería cargar la lista de pedidos cuando el repositorio responde con éxito") {
                // Arrange
                val fakeUsuario = Usuario("uid", "email", Rol(3, "C"), "fecha")
                val fakeCliente = Cliente(1L, fakeUsuario, "user", "phone", emptyList())
                val fakeHistorial = listOf(mockk<PedidoDTO>(), mockk<PedidoDTO>())
                
                coEvery { clienteRepository.currentCliente } returns MutableStateFlow(fakeCliente)
                coEvery { pedidoRepository.getPedidosPorCliente(1L) } returns Result.success(fakeHistorial)

                // Act
                viewModel.cargarHistorialPedidos()

                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.historialPedidos.size shouldBe 2
            }
        }
    }
}