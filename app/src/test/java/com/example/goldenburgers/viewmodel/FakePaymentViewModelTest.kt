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
            pedidoRepository = mockk()
            viewModel = FakePaymentViewModel(pedidoRepository)
        }

        afterTest {
            Dispatchers.resetMain()
        }

        context("Procesamiento de Pago") {
            should("actualizar el estado a SUCCESS si el número de tarjeta es correcto") {
                // Arrange
                val magicCardNumber = "1111-2222-3333-4444"
                val orderId = 99L
                // El estado final para un pago exitoso es 2L
                val successStateId = 2L 

                coEvery { pedidoRepository.cambiarEstadoPedido(orderId, successStateId) } returns Result.success(mockk())

                // Act
                viewModel.processPayment(magicCardNumber, orderId)

                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.paymentResult shouldBe PaymentResult.SUCCESS
                coVerify(exactly = 1) { pedidoRepository.cambiarEstadoPedido(orderId, successStateId) }
            }

            should("actualizar el estado a FAILURE si el número de tarjeta es incorrecto") {
                // Arrange
                val wrongCardNumber = "0000-0000-0000-0000"
                val orderId = 101L
                // El estado final para un pago fallido/cancelado es 7L
                val failureStateId = 7L 

                coEvery { pedidoRepository.cambiarEstadoPedido(orderId, failureStateId) } returns Result.success(mockk())

                // Act
                viewModel.processPayment(wrongCardNumber, orderId)

                // Assert
                val state = viewModel.uiState.value
                state.isLoading shouldBe false
                state.paymentResult shouldBe PaymentResult.FAILURE
                coVerify(exactly = 1) { pedidoRepository.cambiarEstadoPedido(orderId, failureStateId) }
            }

            should("resetPaymentResult debería poner el resultado a nulo") {
                // Arrange: Forzamos un estado inicial
                viewModel.processPayment("1111-2222-3333-4444", 1L)

                // Act: Reseteamos el estado
                viewModel.resetPaymentResult()

                // Assert
                viewModel.uiState.value.paymentResult shouldBe null
            }
        }
    }
}