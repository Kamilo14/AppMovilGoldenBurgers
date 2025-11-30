package com.example.goldenburgers.repository

import com.example.goldenburgers.model.dto.DetallePedidoDTO
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.service.PedidoApiService
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import retrofit2.Response

@ExperimentalCoroutinesApi
class PedidoRepositoryTest : ShouldSpec() {

    private lateinit var networkSource: PedidoNetworkSource
    private lateinit var apiService: PedidoApiService
    private lateinit var repository: PedidoRepository

    init {
        beforeTest {
            networkSource = mockk()
            apiService = mockk()
            repository = PedidoRepository(networkSource)

            // Siempre simulamos que el networkSource devuelve nuestro apiService falso
            coEvery { networkSource.getService() } returns apiService
        }

        context("Crear Pedido") {
            should("debería llamar al servicio de API con el DTO correcto") {
                // Arrange
                val pedidoDto = PedidoDTO(
                    idPedido = null,
                    idCliente = 1L,
                    idEstadoPedido = 1,
                    idMetodoPago = 2,
                    idTipoEntrega = 1,
                    idDireccionEntrega = 10L,
                    montoSubtotal = 10000.0,
                    montoEnvio = 1000.0,
                    montoTotal = 11000.0,
                    fechaPedido = "2025-01-01T12:00:00",
                    notaCliente = "Sin mayonesa",
                    detalles = listOf(
                        DetallePedidoDTO(null, 0, 100L, 2, 5000.0, 10000.0)
                    )
                )
                val fakeResponse = pedidoDto.copy(idPedido = 99L)

                coEvery { apiService.crearPedidoCompleto(any()) } returns fakeResponse

                // Act
                val result = repository.crearPedidoCompleto(pedidoDto)

                // Assert
                result.isSuccess shouldBe true
                result.getOrNull()?.idPedido shouldBe 99L
                coVerify(exactly = 1) { apiService.crearPedidoCompleto(pedidoDto) }
            }
        }

        context("Obtener Pedidos por Cliente") {
            should("debería devolver una lista de pedidos cuando la API responde con éxito") {
                // Arrange
                val fakePedidoList = listOf(mockk<PedidoDTO>(), mockk<PedidoDTO>())
                // [CORRECCIÓN] Envolvemos la lista en un Response.success para que coincida con la firma de la API
                val fakeResponse = Response.success(fakePedidoList)
                coEvery { apiService.getPedidosPorCliente(1L) } returns fakeResponse

                // Act
                val result = repository.getPedidosPorCliente(1L)

                // Assert
                result.isSuccess shouldBe true
                result.getOrNull()?.size shouldBe 2
                coVerify(exactly = 1) { apiService.getPedidosPorCliente(1L) }
            }
        }

        context("Cambiar Estado de Pedido") {
            should("debería llamar al servicio de API para cambiar el estado") {
                 // Arrange
                val fakeUpdatedPedido = mockk<PedidoDTO>()
                coEvery { apiService.cambiarEstadoPedido(any(), any()) } returns fakeUpdatedPedido

                // Act
                val result = repository.cambiarEstadoPedido(idPedido = 99L, idEstado = 2L)

                // Assert
                result.isSuccess shouldBe true
                result.getOrNull() shouldBe fakeUpdatedPedido
                coVerify(exactly = 1) { apiService.cambiarEstadoPedido(99L, 2L) }
            }
        }
    }
}