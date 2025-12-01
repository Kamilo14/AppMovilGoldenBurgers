package com.example.goldenburgers.viewmodel

import com.example.goldenburgers.repository.PedidoRepository
import io.kotest.core.spec.style.ShouldSpec
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

@ExperimentalCoroutinesApi
class FakePaymentViewModelTest : ShouldSpec() {

    private lateinit var pedidoRepository: PedidoRepository
    private lateinit var viewModel: FakePaymentViewModel
    private val testDispatcher = UnconfinedTestDispatcher()

    init {
        beforeTest {
            Dispatchers.setMain(testDispatcher)
            pedidoRepository = mockk(relaxed = true)
            // Comportamiento por defecto para evitar errores si no se define en el test específico
            coEvery { pedidoRepository.cambiarEstadoPedido(any(), any()) } returns Result.success(mockk())
            
            viewModel = FakePaymentViewModel(pedidoRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Procesamiento de Pagos") {
            
            should("procesar pago exitosamente si el número de tarjeta es correcto") {
                // Arrange
                val magicCardNumber = "1111222233334444"
                val orderId = 1L
                
                // Esperamos que si la tarjeta es correcta, se intente pasar al estado 2 (PAGADO)
                coEvery { pedidoRepository.cambiarEstadoPedido(orderId, 2L) } returns Result.success(mockk())

                // Act
                viewModel.processPayment(magicCardNumber, orderId)
                
                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.paymentResult shouldBe PaymentResult.SUCCESS
                
                coVerify(exactly = 1) { pedidoRepository.cambiarEstadoPedido(orderId, 2L) }
            }
            
            should("procesar pago como fallido si el número de tarjeta es incorrecto") {
                // Arrange
                val wrongCardNumber = "0000000000000000"
                val orderId = 1L
                
                // Esperamos que si la tarjeta es incorrecta, se intente pasar al estado 7 (CANCELADO/FALLIDO)
                coEvery { pedidoRepository.cambiarEstadoPedido(orderId, 7L) } returns Result.success(mockk())

                // Act
                viewModel.processPayment(wrongCardNumber, orderId)
                
                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.paymentResult shouldBe PaymentResult.FAILURE
                
                coVerify(exactly = 1) { pedidoRepository.cambiarEstadoPedido(orderId, 7L) }
            }

            should("manejar error del repositorio") {
                // Arrange
                val magicCardNumber = "1111222233334444"
                val orderId = 1L
                val errorMsg = "Error de red simulado"
                
                // Simulamos un fallo en la llamada a la API
                coEvery { pedidoRepository.cambiarEstadoPedido(any(), any()) } returns Result.failure(Exception(errorMsg))

                // Act
                viewModel.processPayment(magicCardNumber, orderId)
                
                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.error shouldBe errorMsg
            }

            should("resetPaymentResult debería limpiar el resultado del pago") {
                // Arrange: Forzamos un estado (simulando pago)
                viewModel.processPayment("1111222233334444", 1L)
                
                // Act
                viewModel.resetPaymentResult()

                // Assert
                viewModel.uiState.value.paymentResult shouldBe null
            }
        }
    }
}
