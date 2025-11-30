package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.repository.PedidoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FakePaymentUiState(
    val isLoading: Boolean = false,
    val paymentResult: PaymentResult? = null, // null, SUCCESS, or FAILURE
    val error: String? = null
)

enum class PaymentResult { SUCCESS, FAILURE }

class FakePaymentViewModel(
    private val pedidoRepository: PedidoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(FakePaymentUiState())
    val uiState: StateFlow<FakePaymentUiState> = _uiState.asStateFlow()

    // El número de tarjeta "mágico" que aceptará el pago.
    private val magicCardNumber = "1111222233334444"

    fun processPayment(cardNumber: String, orderId: Long) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, paymentResult = null, error = null) }

            val isSuccess = cardNumber.filter { it.isDigit() } == magicCardNumber
            val finalState = if (isSuccess) 2L else 7L // 2: Pagado, 7: Cancelado

            val result = pedidoRepository.cambiarEstadoPedido(orderId, finalState)

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            paymentResult = if (isSuccess) PaymentResult.SUCCESS else PaymentResult.FAILURE
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = exception.message) }
                }
            )
        }
    }

    // Para resetear el estado al salir de la pantalla de resultado
    fun resetPaymentResult() {
        _uiState.update { it.copy(paymentResult = null) }
    }
}