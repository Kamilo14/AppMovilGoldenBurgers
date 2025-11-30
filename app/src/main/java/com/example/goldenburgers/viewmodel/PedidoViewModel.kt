package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.dto.DetallePedidoDTO
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.PedidoRepository
import com.example.goldenburgers.view.DeliveryOption
import com.example.goldenburgers.view.PaymentOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PedidoUiState(
    val historialPedidos: List<PedidoDTO> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val ultimoPedidoCreado: PedidoDTO? = null
)

class PedidoViewModel(
    private val pedidoRepository: PedidoRepository,
    private val clienteRepository: ClienteRepository,
    private val authRepository: AuthRepository // [NUEVO] Inyectamos AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidoUiState())
    val uiState: StateFlow<PedidoUiState> = _uiState.asStateFlow()
    
    // [NUEVO] Lógica de seguridad para asegurar que el cliente esté cargado
    private suspend fun ensureClientLoaded(): Boolean {
        if (clienteRepository.currentCliente.value != null) return true

        val firebaseUser = authRepository.getCurrentUser()
        if (firebaseUser == null) {
            _uiState.update { it.copy(isLoading = false, error = "Error: Sesión de usuario no encontrada.") }
            return false
        }

        val result = clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid)
        return result.isSuccess
    }

    fun crearPedido(
        items: List<CartItem>,
        subtotal: Double,
        direccion: DireccionCliente?,
        tipoEntrega: DeliveryOption,
        metodoPago: PaymentOption
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, ultimoPedidoCreado = null) }

            if (!ensureClientLoaded()) return@launch

            val cliente = clienteRepository.currentCliente.value
            if (cliente == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener la información del cliente.") }
                return@launch
            }

            val pedidoDTO = PedidoDTO(
                idPedido = null,
                idCliente = cliente.idCliente,
                idEstadoPedido = 1, // 1: Pendiente de Pago
                idMetodoPago = metodoPago.id,
                idTipoEntrega = tipoEntrega.id,
                idDireccionEntrega = direccion?.idDireccion,
                montoSubtotal = subtotal,
                montoEnvio = tipoEntrega.cost,
                montoTotal = subtotal + tipoEntrega.cost,
                fechaPedido = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).format(Date()),
                notaCliente = null,
                detalles = items.map {
                    DetallePedidoDTO(
                        idDetalle = null,
                        idPedido = 0,
                        idProducto = it.product.idProducto ?: 0,
                        cantidad = it.quantity,
                        precioUnitario = it.product.precioBase,
                        subtotalLinea = it.quantity * it.product.precioBase
                    )
                }
            )

            val result = pedidoRepository.crearPedidoCompleto(pedidoDTO)

            result.fold(
                onSuccess = { nuevoPedido ->
                    _uiState.update { it.copy(isLoading = false, ultimoPedidoCreado = nuevoPedido) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = "Error al crear el pedido: ${exception.message}") }
                }
            )
        }
    }

    fun cargarHistorialPedidos() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            if (!ensureClientLoaded()) return@launch

            val cliente = clienteRepository.currentCliente.value
            if (cliente == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener la información del cliente para cargar el historial.") }
                return@launch
            }

            val result = pedidoRepository.getPedidosPorCliente(cliente.idCliente)
            result.fold(
                onSuccess = { pedidos ->
                    _uiState.update { it.copy(isLoading = false, historialPedidos = pedidos) }
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false, error = "Error al cargar el historial: ${exception.message}") }
                }
            )
        }
    }

    fun resetUltimoPedido() {
        _uiState.update { it.copy(ultimoPedidoCreado = null) }
    }
}