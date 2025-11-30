package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.model.dto.DetallePedidoDTO
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.PedidoRepository
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
    val pedidoCreadoExitosamente: Boolean = false
)

class PedidoViewModel(
    private val pedidoRepository: PedidoRepository,
    private val clienteRepository: ClienteRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PedidoUiState())
    val uiState: StateFlow<PedidoUiState> = _uiState.asStateFlow()

    fun crearPedido(
        items: List<CartItem>,
        total: Double,
        direccion: DireccionCliente
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, pedidoCreadoExitosamente = false) }

            val cliente = clienteRepository.currentCliente.first()
            if (cliente == null) {
                _uiState.update { it.copy(isLoading = false, error = "No se pudo obtener la información del cliente.") }
                return@launch
            }

            // [CORREGIDO] Se usan los nombres de campos correctos de los DTOs
            val pedidoDTO = PedidoDTO(
                idPedido = null,
                idCliente = cliente.idCliente,
                idEstadoPedido = 1, // Asumimos 1: Pendiente
                idMetodoPago = 1,   // Asumimos 1: Por definir o un método por defecto
                idTipoEntrega = 1,  // Asumimos 1: Despacho a domicilio
                idDireccionEntrega = direccion.idDireccion,
                montoSubtotal = total, // Aquí debería ir el subtotal sin envío
                montoEnvio = 0.0, // Hardcodeado por ahora
                montoTotal = total, // Aquí debería ser subtotal + envío
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
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false, pedidoCreadoExitosamente = true) }
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

            val cliente = clienteRepository.currentCliente.first()
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

    fun resetPedidoCreado() {
        _uiState.update { it.copy(pedidoCreadoExitosamente = false) }
    }
}