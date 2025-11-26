# GUÍA DE INTEGRACIÓN - PARTE 3: PEDIDOS Y PAGOS

## Golden Burgers - App Android + Backend Microservicios

---

## ÍNDICE PARTE 3

1. [Paso 17: Implementar Creación de Pedidos](#paso-17-implementar-creación-de-pedidos)
2. [Paso 18: Implementar Pago con Mercado Pago](#paso-18-implementar-pago-con-mercado-pago)
3. [Paso 19: Implementar Historial de Pedidos](#paso-19-implementar-historial-de-pedidos)
4. [Paso 20: Agregar Navegación a Historial](#paso-20-agregar-navegación-a-historial)
5. [Paso 21: Simplificar Formulario de Registro](#paso-21-simplificar-formulario-de-registro)
6. [Paso 22: Manejo de Errores y Estados de Carga](#paso-22-manejo-de-errores-y-estados-de-carga)
7. [Paso 23: Configurar URL Base para Producción](#paso-23-configurar-url-base-para-producción)
8. [Resumen Final y Checklist](#resumen-final-y-checklist)

---

## PASO 17: IMPLEMENTAR CREACIÓN DE PEDIDOS

### Flujo de Checkout

```
┌─────────────────────────────────────────────────────────────────┐
│                    FLUJO DE CHECKOUT                            │
├─────────────────────────────────────────────────────────────────┤
│  1. Usuario tiene productos en el carrito                       │
│                          ↓                                      │
│  2. Presiona "Realizar Pedido" en CartScreen                    │
│                          ↓                                      │
│  3. Navega a CheckoutScreen                                     │
│                          ↓                                      │
│  4. Selecciona tipo de entrega (Delivery / Retiro)              │
│                          ↓                                      │
│  5. Si Delivery: Selecciona dirección de entrega                │
│                          ↓                                      │
│  6. Selecciona método de pago                                   │
│                          ↓                                      │
│  7. Agrega notas opcionales ("Sin cebolla")                     │
│                          ↓                                      │
│  8. Confirma pedido → POST /api/pedidos/completo                │
│                          ↓                                      │
│  9. Si Mercado Pago: POST /api/pagos/crear-preferencia          │
│                          ↓                                      │
│ 10. Abre WebView/Browser con URL de pago                        │
│                          ↓                                      │
│ 11. Usuario paga en Mercado Pago                                │
│                          ↓                                      │
│ 12. Webhook actualiza estado del pedido                         │
│                          ↓                                      │
│ 13. Usuario ve confirmación / Navega a historial                │
└─────────────────────────────────────────────────────────────────┘
```

### Archivo: `data/repository/OrderRepository.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.data.repository

import com.example.goldenburgers.data.remote.ApiClient
import com.example.goldenburgers.data.remote.ApiService
import com.example.goldenburgers.data.remote.dto.request.*
import com.example.goldenburgers.data.remote.dto.response.*
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.viewmodel.CartItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Repository para gestión de pedidos y pagos.
 */
class OrderRepository(
    private val sessionManager: SessionManager
) {
    private val apiService: ApiService by lazy {
        ApiClient.getApiService(sessionManager)
    }

    sealed class Result<out T> {
        data class Success<T>(val data: T) : Result<T>()
        data class Error(val message: String) : Result<Nothing>()
    }

    // ============================================
    // PEDIDOS
    // ============================================

    /**
     * Crea un pedido completo con todos los detalles.
     *
     * @param cartItems Lista de items del carrito
     * @param idTipoEntrega 1 = Delivery, 2 = Retiro en local
     * @param idDireccionEntrega ID de la dirección (requerido si delivery)
     * @param idMetodoPago 1 = Mercado Pago, 2 = Efectivo, etc.
     * @param notasCliente Notas especiales ("Sin cebolla", etc.)
     * @param costoEnvio Costo de envío (0 si retiro)
     */
    suspend fun crearPedido(
        cartItems: List<CartItem>,
        idTipoEntrega: Long,
        idDireccionEntrega: Long?,
        idMetodoPago: Long,
        notasCliente: String?,
        costoEnvio: Double = 0.0
    ): Result<PedidoResponse> = withContext(Dispatchers.IO) {
        try {
            val clientId = sessionManager.clientIdFlow.first()
                ?: return@withContext Result.Error("No hay sesión activa")

            // Calcular montos
            val subtotal = cartItems.sumOf { it.product.precio * it.quantity }
            val total = subtotal + costoEnvio

            // Crear detalles del pedido
            val detalles = cartItems.map { item ->
                DetallePedidoRequest(
                    idProducto = item.product.id,
                    cantidad = item.quantity,
                    precioUnitario = item.product.precio,
                    subtotalLinea = item.product.precio * item.quantity
                )
            }

            val request = CrearPedidoRequest(
                idCliente = clientId,
                idEstadoPedido = 1, // Pendiente de pago
                idMetodoPago = idMetodoPago,
                idTipoEntrega = idTipoEntrega,
                idDireccionEntrega = idDireccionEntrega,
                montoSubtotal = subtotal,
                montoEnvio = costoEnvio,
                montoTotal = total,
                notasCliente = notasCliente,
                detalles = detalles
            )

            val response = apiService.crearPedido(request)

            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.Error("Error al crear pedido: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene el historial de pedidos del cliente actual.
     */
    suspend fun getHistorialPedidos(): Result<List<PedidoResponse>> = withContext(Dispatchers.IO) {
        try {
            val clientId = sessionManager.clientIdFlow.first()
                ?: return@withContext Result.Error("No hay sesión activa")

            val response = apiService.getPedidosCliente(clientId)

            if (response.isSuccessful) {
                // Ordenar por fecha descendente (más recientes primero)
                val pedidos = response.body() ?: emptyList()
                Result.Success(pedidos.sortedByDescending { it.fechaPedido })
            } else if (response.code() == 204) {
                // Sin contenido = no hay pedidos
                Result.Success(emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene detalle de un pedido específico.
     */
    suspend fun getPedido(idPedido: Long): Result<PedidoResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPedidoPorId(idPedido)

            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Cancela un pedido (solo si está pendiente).
     */
    suspend fun cancelarPedido(idPedido: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.cancelarPedido(idPedido)

            if (response.isSuccessful) {
                Result.Success(Unit)
            } else {
                Result.Error("No se puede cancelar el pedido")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    // ============================================
    // PAGOS
    // ============================================

    /**
     * Crea una preferencia de pago en Mercado Pago.
     *
     * @param pedido El pedido para el cual crear el pago
     * @return PagoResponse con la URL de pago
     */
    suspend fun crearPreferenciaPago(
        pedido: PedidoResponse
    ): Result<PagoResponse> = withContext(Dispatchers.IO) {
        try {
            val email = sessionManager.loggedInUserEmailFlow.first()
                ?: return@withContext Result.Error("No hay sesión activa")

            val request = CrearPagoRequest(
                idPedido = pedido.idPedido,
                montoPago = pedido.montoTotal,
                descripcion = "Pago pedido #${pedido.idPedido} - Golden Burgers",
                email = email
            )

            val response = apiService.crearPreferenciaPago(request)

            if (response.isSuccessful) {
                Result.Success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string()
                Result.Error("Error al crear pago: ${response.code()} - $errorBody")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    /**
     * Obtiene el estado de pago de un pedido.
     */
    suspend fun getPagosPedido(idPedido: Long): Result<List<PagoResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPagosPorPedido(idPedido)

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                Result.Error("Error ${response.code()}")
            }
        } catch (e: Exception) {
            Result.Error(e.message ?: "Error de conexión")
        }
    }

    // ============================================
    // TIPOS Y MÉTODOS
    // ============================================

    /**
     * Obtiene tipos de entrega disponibles.
     */
    suspend fun getTiposEntrega(): Result<List<TipoEntregaResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTiposEntrega()

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                // Retornar valores por defecto si falla
                Result.Success(
                    listOf(
                        TipoEntregaResponse(1, "Delivery"),
                        TipoEntregaResponse(2, "Retiro en local")
                    )
                )
            }
        } catch (e: Exception) {
            // Retornar valores por defecto
            Result.Success(
                listOf(
                    TipoEntregaResponse(1, "Delivery"),
                    TipoEntregaResponse(2, "Retiro en local")
                )
            )
        }
    }

    /**
     * Obtiene métodos de pago disponibles.
     */
    suspend fun getMetodosPago(): Result<List<MetodoPagoResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getMetodosPago()

            if (response.isSuccessful) {
                Result.Success(response.body() ?: emptyList())
            } else {
                // Retornar valores por defecto
                Result.Success(
                    listOf(
                        MetodoPagoResponse(1, "Mercado Pago"),
                        MetodoPagoResponse(2, "Efectivo")
                    )
                )
            }
        } catch (e: Exception) {
            Result.Success(
                listOf(
                    MetodoPagoResponse(1, "Mercado Pago"),
                    MetodoPagoResponse(2, "Efectivo")
                )
            )
        }
    }
}
```

### Archivo: `viewmodel/CheckoutViewModel.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.remote.dto.response.*
import com.example.goldenburgers.data.repository.AddressRepository
import com.example.goldenburgers.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Estado UI del checkout.
 */
data class CheckoutUiState(
    // Datos del carrito (pasados desde CatalogViewModel)
    val cartItems: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,

    // Opciones disponibles
    val tiposEntrega: List<TipoEntregaResponse> = emptyList(),
    val metodosPago: List<MetodoPagoResponse> = emptyList(),
    val direcciones: List<DireccionResponse> = emptyList(),

    // Selecciones del usuario
    val selectedTipoEntregaId: Long = 1, // Default: Delivery
    val selectedMetodoPagoId: Long = 1,  // Default: Mercado Pago
    val selectedDireccionId: Long? = null,
    val notasCliente: String = "",

    // Cálculos
    val costoEnvio: Double = 2000.0, // Costo fijo de envío

    // Estados
    val isLoading: Boolean = false,
    val isProcessing: Boolean = false,
    val errorMessage: String? = null,

    // Resultado
    val pedidoCreado: PedidoResponse? = null,
    val pagoResponse: PagoResponse? = null
) {
    // Total calculado
    val total: Double
        get() = if (selectedTipoEntregaId == 1L) {
            subtotal + costoEnvio // Delivery
        } else {
            subtotal // Retiro
        }

    // Indica si es delivery
    val isDelivery: Boolean
        get() = selectedTipoEntregaId == 1L

    // Valida si se puede continuar
    val canProceed: Boolean
        get() = cartItems.isNotEmpty() &&
                (!isDelivery || selectedDireccionId != null)
}

/**
 * ViewModel para el proceso de checkout.
 */
class CheckoutViewModel(
    private val orderRepository: OrderRepository,
    private val addressRepository: AddressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    init {
        loadCheckoutData()
    }

    /**
     * Inicializa el checkout con los items del carrito.
     */
    fun initializeWithCart(cartItems: List<CartItem>) {
        val subtotal = cartItems.sumOf { it.product.precio * it.quantity }
        _uiState.update {
            it.copy(
                cartItems = cartItems,
                subtotal = subtotal
            )
        }
    }

    /**
     * Carga datos necesarios para el checkout.
     */
    private fun loadCheckoutData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Cargar tipos de entrega
            when (val result = orderRepository.getTiposEntrega()) {
                is OrderRepository.Result.Success -> {
                    _uiState.update { it.copy(tiposEntrega = result.data) }
                }
                is OrderRepository.Result.Error -> { /* Usar defaults */ }
            }

            // Cargar métodos de pago
            when (val result = orderRepository.getMetodosPago()) {
                is OrderRepository.Result.Success -> {
                    _uiState.update { it.copy(metodosPago = result.data) }
                }
                is OrderRepository.Result.Error -> { /* Usar defaults */ }
            }

            // Cargar direcciones del cliente
            when (val result = addressRepository.getDirecciones()) {
                is AddressRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            direcciones = result.data,
                            // Seleccionar primera dirección por defecto
                            selectedDireccionId = result.data.firstOrNull()?.idDireccion
                        )
                    }
                }
                is AddressRepository.Result.Error -> { /* Sin direcciones */ }
            }

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    // ============================================
    // ACTUALIZADORES
    // ============================================

    fun selectTipoEntrega(id: Long) {
        _uiState.update { it.copy(selectedTipoEntregaId = id) }
    }

    fun selectMetodoPago(id: Long) {
        _uiState.update { it.copy(selectedMetodoPagoId = id) }
    }

    fun selectDireccion(id: Long) {
        _uiState.update { it.copy(selectedDireccionId = id) }
    }

    fun updateNotas(notas: String) {
        _uiState.update { it.copy(notasCliente = notas) }
    }

    // ============================================
    // PROCESAR PEDIDO
    // ============================================

    /**
     * Procesa el pedido completo.
     *
     * @param onPagoRequerido Callback con URL de pago si es Mercado Pago
     * @param onPedidoCompletado Callback si el pedido se completa sin pago online
     * @param onError Callback en caso de error
     */
    fun procesarPedido(
        onPagoRequerido: (String) -> Unit,
        onPedidoCompletado: (PedidoResponse) -> Unit,
        onError: (String) -> Unit
    ) {
        val state = _uiState.value

        // Validar
        if (state.cartItems.isEmpty()) {
            onError("El carrito está vacío")
            return
        }

        if (state.isDelivery && state.selectedDireccionId == null) {
            onError("Selecciona una dirección de entrega")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isProcessing = true, errorMessage = null) }

            // 1. Crear pedido
            val resultPedido = orderRepository.crearPedido(
                cartItems = state.cartItems,
                idTipoEntrega = state.selectedTipoEntregaId,
                idDireccionEntrega = if (state.isDelivery) state.selectedDireccionId else null,
                idMetodoPago = state.selectedMetodoPagoId,
                notasCliente = state.notasCliente.ifBlank { null },
                costoEnvio = if (state.isDelivery) state.costoEnvio else 0.0
            )

            when (resultPedido) {
                is OrderRepository.Result.Success -> {
                    val pedido = resultPedido.data
                    _uiState.update { it.copy(pedidoCreado = pedido) }

                    // 2. Si es Mercado Pago, crear preferencia de pago
                    if (state.selectedMetodoPagoId == 1L) { // Mercado Pago
                        val resultPago = orderRepository.crearPreferenciaPago(pedido)

                        when (resultPago) {
                            is OrderRepository.Result.Success -> {
                                val pago = resultPago.data
                                _uiState.update {
                                    it.copy(
                                        isProcessing = false,
                                        pagoResponse = pago
                                    )
                                }

                                // Callback con URL de pago
                                pago.urlPago?.let { url ->
                                    onPagoRequerido(url)
                                } ?: onError("No se obtuvo URL de pago")
                            }

                            is OrderRepository.Result.Error -> {
                                _uiState.update {
                                    it.copy(
                                        isProcessing = false,
                                        errorMessage = resultPago.message
                                    )
                                }
                                onError(resultPago.message)
                            }
                        }
                    } else {
                        // Otros métodos de pago (efectivo, etc.)
                        _uiState.update { it.copy(isProcessing = false) }
                        onPedidoCompletado(pedido)
                    }
                }

                is OrderRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isProcessing = false,
                            errorMessage = resultPedido.message
                        )
                    }
                    onError(resultPedido.message)
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun clearPedido() {
        _uiState.update {
            it.copy(
                pedidoCreado = null,
                pagoResponse = null
            )
        }
    }
}
```

### Archivo: `view/CheckoutScreen.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.view

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.goldenburgers.data.remote.dto.response.DireccionResponse
import com.example.goldenburgers.viewmodel.CartItem
import com.example.goldenburgers.viewmodel.CheckoutUiState
import com.example.goldenburgers.viewmodel.CheckoutViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel,
    cartItems: List<CartItem>,
    onBackClick: () -> Unit,
    onOrderCompleted: () -> Unit
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // Inicializar con items del carrito
    LaunchedEffect(cartItems) {
        viewModel.initializeWithCart(cartItems)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Confirmar Pedido") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Resumen del pedido
                item {
                    OrderSummaryCard(cartItems = uiState.cartItems)
                }

                // Tipo de entrega
                item {
                    DeliveryTypeSection(
                        tiposEntrega = uiState.tiposEntrega,
                        selectedId = uiState.selectedTipoEntregaId,
                        onSelect = viewModel::selectTipoEntrega
                    )
                }

                // Dirección (solo si es delivery)
                if (uiState.isDelivery) {
                    item {
                        AddressSection(
                            direcciones = uiState.direcciones,
                            selectedId = uiState.selectedDireccionId,
                            onSelect = viewModel::selectDireccion
                        )
                    }
                }

                // Método de pago
                item {
                    PaymentMethodSection(
                        metodosPago = uiState.metodosPago,
                        selectedId = uiState.selectedMetodoPagoId,
                        onSelect = viewModel::selectMetodoPago
                    )
                }

                // Notas
                item {
                    NotesSection(
                        notas = uiState.notasCliente,
                        onNotasChange = viewModel::updateNotas
                    )
                }

                // Totales
                item {
                    TotalsCard(
                        subtotal = uiState.subtotal,
                        costoEnvio = if (uiState.isDelivery) uiState.costoEnvio else 0.0,
                        total = uiState.total
                    )
                }

                // Botón de confirmar
                item {
                    Button(
                        onClick = {
                            viewModel.procesarPedido(
                                onPagoRequerido = { url ->
                                    // Abrir URL de Mercado Pago en navegador
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                                    context.startActivity(intent)
                                },
                                onPedidoCompletado = {
                                    onOrderCompleted()
                                },
                                onError = { /* Se muestra en UI */ }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        enabled = uiState.canProceed && !uiState.isProcessing
                    ) {
                        if (uiState.isProcessing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Default.ShoppingCart, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Confirmar Pedido - $${uiState.total.toInt()}")
                        }
                    }
                }

                // Error
                uiState.errorMessage?.let { error ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Error,
                                    null,
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = error,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(onClick = viewModel::clearError) {
                                    Icon(Icons.Default.Close, "Cerrar")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderSummaryCard(cartItems: List<CartItem>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Resumen del Pedido",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            cartItems.forEach { item ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${item.quantity}x ${item.product.nombre}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        text = "$${(item.product.precio * item.quantity).toInt()}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun DeliveryTypeSection(
    tiposEntrega: List<com.example.goldenburgers.data.remote.dto.response.TipoEntregaResponse>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Tipo de Entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            tiposEntrega.forEach { tipo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = tipo.idTipoEntrega == selectedId,
                            onClick = { onSelect(tipo.idTipoEntrega) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = tipo.idTipoEntrega == selectedId,
                        onClick = { onSelect(tipo.idTipoEntrega) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (tipo.idTipoEntrega == 1L) {
                            Icons.Default.DeliveryDining
                        } else {
                            Icons.Default.Store
                        },
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = tipo.nombreTipoEntrega)
                }
            }
        }
    }
}

@Composable
fun AddressSection(
    direcciones: List<DireccionResponse>,
    selectedId: Long?,
    onSelect: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dirección de Entrega",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (direcciones.isEmpty()) {
                Text(
                    text = "No tienes direcciones guardadas",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
                // TODO: Agregar botón para crear dirección
            } else {
                direcciones.forEach { direccion ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = direccion.idDireccion == selectedId,
                                onClick = { onSelect(direccion.idDireccion) }
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = direccion.idDireccion == selectedId,
                            onClick = { onSelect(direccion.idDireccion) }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = direccion.alias ?: "Dirección",
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = direccion.direccion,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PaymentMethodSection(
    metodosPago: List<com.example.goldenburgers.data.remote.dto.response.MetodoPagoResponse>,
    selectedId: Long,
    onSelect: (Long) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Método de Pago",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            metodosPago.forEach { metodo ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = metodo.idMetodoPago == selectedId,
                            onClick = { onSelect(metodo.idMetodoPago) }
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = metodo.idMetodoPago == selectedId,
                        onClick = { onSelect(metodo.idMetodoPago) }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = if (metodo.idMetodoPago == 1L) {
                            Icons.Default.CreditCard
                        } else {
                            Icons.Default.Money
                        },
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = metodo.nombreMetodoPago)
                }
            }
        }
    }
}

@Composable
fun NotesSection(
    notas: String,
    onNotasChange: (String) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Notas del Pedido (Opcional)",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = notas,
                onValueChange = onNotasChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Ej: Sin cebolla, extra salsa...") },
                maxLines = 3
            )
        }
    }
}

@Composable
fun TotalsCard(
    subtotal: Double,
    costoEnvio: Double,
    total: Double
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Subtotal")
                Text("$${subtotal.toInt()}")
            }

            if (costoEnvio > 0) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Envío")
                    Text("$${costoEnvio.toInt()}")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Total",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = "$${total.toInt()}",
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
```

---

## PASO 18: IMPLEMENTAR PAGO CON MERCADO PAGO

### Flujo de Pago Mercado Pago

```
┌──────────────────────────────────────────────────────────────┐
│                FLUJO MERCADO PAGO                            │
├──────────────────────────────────────────────────────────────┤
│  1. App crea pedido en backend                               │
│                     ↓                                        │
│  2. Backend crea preferencia en Mercado Pago                 │
│                     ↓                                        │
│  3. Backend retorna URL de pago                              │
│                     ↓                                        │
│  4. App abre URL en navegador externo                        │
│                     ↓                                        │
│  5. Usuario paga en Mercado Pago                             │
│                     ↓                                        │
│  6. Mercado Pago envía webhook al backend                    │
│                     ↓                                        │
│  7. Backend actualiza estado del pedido                      │
│                     ↓                                        │
│  8. Usuario vuelve a la app                                  │
│                     ↓                                        │
│  9. App consulta estado del pedido (polling o push)          │
└──────────────────────────────────────────────────────────────┘
```

### Manejo de Retorno desde Mercado Pago

Opción 1: **Deep Link** (Recomendado)

Configurar en `AndroidManifest.xml`:

```xml
<activity
    android:name=".MainActivity"
    android:exported="true"
    android:launchMode="singleTask">

    <intent-filter>
        <action android:name="android.intent.action.MAIN" />
        <category android:name="android.intent.category.LAUNCHER" />
    </intent-filter>

    <!-- Deep Link para retorno de Mercado Pago -->
    <intent-filter android:autoVerify="true">
        <action android:name="android.intent.action.VIEW" />
        <category android:name="android.intent.category.DEFAULT" />
        <category android:name="android.intent.category.BROWSABLE" />
        <data
            android:scheme="goldenburgers"
            android:host="payment" />
    </intent-filter>

</activity>
```

### Manejar Deep Link en MainActivity

```kotlin
// MainActivity.kt
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            GoldenBurgersTheme {
                val navController = rememberNavController()

                // Manejar deep link de pago
                LaunchedEffect(Unit) {
                    handlePaymentDeepLink(intent, navController)
                }

                AppNavigation(navController = navController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // Manejar cuando la app ya está abierta
        setIntent(intent)
    }

    private fun handlePaymentDeepLink(intent: Intent?, navController: NavController) {
        intent?.data?.let { uri ->
            if (uri.scheme == "goldenburgers" && uri.host == "payment") {
                // Parámetros que envía Mercado Pago
                val status = uri.getQueryParameter("status") // approved, rejected, pending
                val paymentId = uri.getQueryParameter("payment_id")
                val externalReference = uri.getQueryParameter("external_reference") // ID pedido

                when (status) {
                    "approved" -> {
                        // Navegar a pantalla de éxito
                        navController.navigate("order_success/$externalReference")
                    }
                    "rejected" -> {
                        // Navegar a pantalla de error
                        navController.navigate("order_failed/$externalReference")
                    }
                    "pending" -> {
                        // Navegar a pantalla de pendiente
                        navController.navigate("order_pending/$externalReference")
                    }
                }
            }
        }
    }
}
```

### Configurar URLs de Retorno en Backend

En el backend (gestionpedido), al crear la preferencia de Mercado Pago, configurar las URLs de retorno:

```java
// En el servicio de Mercado Pago del backend
preference.setBackUrls(
    new BackUrls()
        .setSuccess("goldenburgers://payment?status=approved")
        .setFailure("goldenburgers://payment?status=rejected")
        .setPending("goldenburgers://payment?status=pending")
);
```

---

## PASO 19: IMPLEMENTAR HISTORIAL DE PEDIDOS

### Archivo: `viewmodel/OrderHistoryViewModel.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.data.remote.dto.response.PedidoResponse
import com.example.goldenburgers.data.repository.OrderRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class OrderHistoryUiState(
    val pedidos: List<PedidoResponse> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val errorMessage: String? = null,
    val selectedPedido: PedidoResponse? = null
)

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    init {
        loadOrders()
    }

    fun loadOrders() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            when (val result = orderRepository.getHistorialPedidos()) {
                is OrderRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            pedidos = result.data,
                            isLoading = false
                        )
                    }
                }
                is OrderRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }

            when (val result = orderRepository.getHistorialPedidos()) {
                is OrderRepository.Result.Success -> {
                    _uiState.update {
                        it.copy(
                            pedidos = result.data,
                            isRefreshing = false
                        )
                    }
                }
                is OrderRepository.Result.Error -> {
                    _uiState.update {
                        it.copy(
                            isRefreshing = false,
                            errorMessage = result.message
                        )
                    }
                }
            }
        }
    }

    fun selectPedido(pedido: PedidoResponse) {
        _uiState.update { it.copy(selectedPedido = pedido) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedPedido = null) }
    }

    fun cancelarPedido(idPedido: Long) {
        viewModelScope.launch {
            when (val result = orderRepository.cancelarPedido(idPedido)) {
                is OrderRepository.Result.Success -> {
                    // Recargar lista
                    loadOrders()
                }
                is OrderRepository.Result.Error -> {
                    _uiState.update { it.copy(errorMessage = result.message) }
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
```

### Archivo: `view/OrderHistoryScreen.kt` (NUEVO)

```kotlin
package com.example.goldenburgers.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.goldenburgers.data.remote.dto.response.PedidoResponse
import com.example.goldenburgers.viewmodel.OrderHistoryViewModel
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    viewModel: OrderHistoryViewModel,
    onBackClick: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Pedidos") },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::refresh) {
                        Icon(Icons.Default.Refresh, "Actualizar")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = viewModel::refresh,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.pedidos.isEmpty() -> {
                    EmptyOrdersState()
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.pedidos) { pedido ->
                            OrderCard(
                                pedido = pedido,
                                onClick = { viewModel.selectPedido(pedido) }
                            )
                        }
                    }
                }
            }
        }

        // Diálogo de detalle
        uiState.selectedPedido?.let { pedido ->
            OrderDetailDialog(
                pedido = pedido,
                onDismiss = viewModel::clearSelection,
                onCancel = {
                    viewModel.cancelarPedido(pedido.idPedido)
                    viewModel.clearSelection()
                }
            )
        }

        // Snackbar de error
        uiState.errorMessage?.let { error ->
            LaunchedEffect(error) {
                // Mostrar snackbar
            }
        }
    }
}

@Composable
fun EmptyOrdersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Receipt,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No tienes pedidos aún",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "Realiza tu primer pedido",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun OrderCard(
    pedido: PedidoResponse,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header con número y estado
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Pedido #${pedido.idPedido}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )

                OrderStatusChip(
                    estado = pedido.estadoPedido?.nombreEstado ?: "Desconocido",
                    idEstado = pedido.estadoPedido?.idEstadoPedido ?: 0
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Fecha
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CalendarToday,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDate(pedido.fechaPedido),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Tipo de entrega
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (pedido.tipoEntrega?.idTipoEntrega == 1L) {
                        Icons.Default.DeliveryDining
                    } else {
                        Icons.Default.Store
                    },
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = pedido.tipoEntrega?.nombreTipoEntrega ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(8.dp))

            // Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${pedido.detalles?.size ?: 0} productos",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "$${pedido.montoTotal.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun OrderStatusChip(estado: String, idEstado: Long) {
    val (color, icon) = when (idEstado) {
        1L -> MaterialTheme.colorScheme.tertiary to Icons.Default.Schedule // Pendiente
        2L -> Color(0xFF4CAF50) to Icons.Default.CheckCircle // Pagado
        3L -> Color(0xFFFF9800) to Icons.Default.Restaurant // En preparación
        4L -> Color(0xFF2196F3) to Icons.Default.DeliveryDining // En camino
        5L -> Color(0xFF4CAF50) to Icons.Default.Done // Entregado
        6L -> MaterialTheme.colorScheme.error to Icons.Default.Cancel // Cancelado
        else -> MaterialTheme.colorScheme.outline to Icons.Default.Info
    }

    AssistChip(
        onClick = {},
        label = { Text(estado, style = MaterialTheme.typography.labelSmall) },
        leadingIcon = {
            Icon(icon, null, modifier = Modifier.size(16.dp))
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = color.copy(alpha = 0.1f),
            labelColor = color,
            leadingIconContentColor = color
        )
    )
}

@Composable
fun OrderDetailDialog(
    pedido: PedidoResponse,
    onDismiss: () -> Unit,
    onCancel: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pedido #${pedido.idPedido}")
        },
        text = {
            Column {
                // Estado
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estado: ", fontWeight = FontWeight.Bold)
                    OrderStatusChip(
                        estado = pedido.estadoPedido?.nombreEstado ?: "",
                        idEstado = pedido.estadoPedido?.idEstadoPedido ?: 0
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Fecha
                Text("Fecha: ${formatDate(pedido.fechaPedido)}")

                Spacer(modifier = Modifier.height(8.dp))

                // Productos
                Text("Productos:", fontWeight = FontWeight.Bold)
                pedido.detalles?.forEach { detalle ->
                    Text("  ${detalle.cantidad}x ${detalle.nombreProducto ?: "Producto"}")
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Dirección si es delivery
                if (pedido.tipoEntrega?.idTipoEntrega == 1L && pedido.direccionEntrega != null) {
                    Text("Dirección:", fontWeight = FontWeight.Bold)
                    Text(pedido.direccionEntrega.direccion)
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Totales
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Subtotal:")
                    Text("$${pedido.montoSubtotal.toInt()}")
                }
                if (pedido.montoEnvio > 0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Envío:")
                        Text("$${pedido.montoEnvio.toInt()}")
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Total:", fontWeight = FontWeight.Bold)
                    Text(
                        "$${pedido.montoTotal.toInt()}",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                // Notas
                if (!pedido.notaCliente.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Notas:", fontWeight = FontWeight.Bold)
                    Text(pedido.notaCliente)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cerrar")
            }
        },
        dismissButton = {
            // Solo mostrar cancelar si está pendiente
            if (pedido.estadoPedido?.idEstadoPedido == 1L) {
                TextButton(
                    onClick = onCancel,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Cancelar Pedido")
                }
            }
        }
    )
}

private fun formatDate(dateString: String): String {
    return try {
        val inputFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
        val outputFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
        val date = LocalDateTime.parse(dateString, inputFormatter)
        date.format(outputFormatter)
    } catch (e: Exception) {
        dateString
    }
}
```

---

## PASO 20: AGREGAR NAVEGACIÓN A HISTORIAL

### Actualizar: `navigation/AppScreens.kt`

```kotlin
package com.example.goldenburgers.navigation

/**
 * Definición de rutas de navegación.
 */
sealed class AppScreens(val route: String) {
    // Flujo de autenticación
    object Welcome : AppScreens("welcome")
    object Login : AppScreens("login")
    object RegisterStep1 : AppScreens("register_step1")
    object RegisterStep2 : AppScreens("register_step2")
    object RegisterStep3 : AppScreens("register_step3")
    object RegisterStep4 : AppScreens("register_step4")
    object RegisterStep5 : AppScreens("register_step5")

    // Flujo principal
    object MainFlow : AppScreens("main_flow")
    object Home : AppScreens("home")
    object Favorites : AppScreens("favorites")
    object Cart : AppScreens("cart")
    object Profile : AppScreens("profile")

    // NUEVAS pantallas
    object EditProfile : AppScreens("edit_profile")
    object Checkout : AppScreens("checkout")
    object OrderHistory : AppScreens("order_history")
    object OrderSuccess : AppScreens("order_success/{orderId}") {
        fun createRoute(orderId: Long) = "order_success/$orderId"
    }
    object OrderFailed : AppScreens("order_failed/{orderId}") {
        fun createRoute(orderId: Long) = "order_failed/$orderId"
    }
    object OrderPending : AppScreens("order_pending/{orderId}") {
        fun createRoute(orderId: Long) = "order_pending/$orderId"
    }
}
```

### Actualizar: `navigation/BottomNavItem.kt`

Agregar opción de historial si se desea en la barra inferior:

```kotlin
package com.example.goldenburgers.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Items de la barra de navegación inferior.
 */
sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : BottomNavItem(
        route = AppScreens.Home.route,
        title = "Inicio",
        selectedIcon = Icons.Filled.Home,
        unselectedIcon = Icons.Outlined.Home
    )

    object Favorites : BottomNavItem(
        route = AppScreens.Favorites.route,
        title = "Favoritos",
        selectedIcon = Icons.Filled.Favorite,
        unselectedIcon = Icons.Outlined.FavoriteBorder
    )

    object Cart : BottomNavItem(
        route = AppScreens.Cart.route,
        title = "Carrito",
        selectedIcon = Icons.Filled.ShoppingCart,
        unselectedIcon = Icons.Outlined.ShoppingCart
    )

    // NUEVO: Agregar historial de pedidos
    object Orders : BottomNavItem(
        route = AppScreens.OrderHistory.route,
        title = "Pedidos",
        selectedIcon = Icons.Filled.Receipt,
        unselectedIcon = Icons.Outlined.Receipt
    )

    object Profile : BottomNavItem(
        route = AppScreens.Profile.route,
        title = "Perfil",
        selectedIcon = Icons.Filled.Person,
        unselectedIcon = Icons.Outlined.Person
    )

    companion object {
        val items = listOf(Home, Favorites, Cart, Orders, Profile)
    }
}
```

### Actualizar: `navigation/AppNavigation.kt`

Agregar las nuevas rutas:

```kotlin
// Dentro del NavHost, agregar:

// Checkout
composable(AppScreens.Checkout.route) {
    val catalogViewModel: CatalogViewModel = // obtener del parent
    val checkoutViewModel: CheckoutViewModel = viewModel(
        factory = CheckoutViewModelFactory(context)
    )

    CheckoutScreen(
        viewModel = checkoutViewModel,
        cartItems = catalogViewModel.uiState.collectAsState().value.cartItems,
        onBackClick = { navController.popBackStack() },
        onOrderCompleted = {
            catalogViewModel.clearCart()
            navController.navigate(AppScreens.OrderHistory.route) {
                popUpTo(AppScreens.Cart.route) { inclusive = true }
            }
        }
    )
}

// Historial de pedidos
composable(AppScreens.OrderHistory.route) {
    val viewModel: OrderHistoryViewModel = viewModel(
        factory = OrderHistoryViewModelFactory(context)
    )

    OrderHistoryScreen(
        viewModel = viewModel,
        onBackClick = { navController.popBackStack() }
    )
}

// Pantalla de éxito
composable(
    route = AppScreens.OrderSuccess.route,
    arguments = listOf(navArgument("orderId") { type = NavType.LongType })
) { backStackEntry ->
    val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L

    OrderResultScreen(
        orderId = orderId,
        isSuccess = true,
        onContinue = {
            navController.navigate(AppScreens.Home.route) {
                popUpTo(AppScreens.MainFlow.route)
            }
        },
        onViewOrders = {
            navController.navigate(AppScreens.OrderHistory.route)
        }
    )
}

// Pantalla de fallo
composable(
    route = AppScreens.OrderFailed.route,
    arguments = listOf(navArgument("orderId") { type = NavType.LongType })
) { backStackEntry ->
    val orderId = backStackEntry.arguments?.getLong("orderId") ?: 0L

    OrderResultScreen(
        orderId = orderId,
        isSuccess = false,
        onContinue = { navController.popBackStack() },
        onViewOrders = {
            navController.navigate(AppScreens.OrderHistory.route)
        }
    )
}
```

### Archivo: `view/OrderResultScreen.kt` (NUEVO)

```kotlin
@Composable
fun OrderResultScreen(
    orderId: Long,
    isSuccess: Boolean,
    onContinue: () -> Unit,
    onViewOrders: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Icono
        Icon(
            imageVector = if (isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
            contentDescription = null,
            modifier = Modifier.size(120.dp),
            tint = if (isSuccess) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Título
        Text(
            text = if (isSuccess) "¡Pago Exitoso!" else "Pago Fallido",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Subtítulo
        Text(
            text = if (isSuccess) {
                "Tu pedido #$orderId ha sido procesado correctamente"
            } else {
                "Hubo un problema con tu pago. Intenta nuevamente."
            },
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botones
        Button(
            onClick = onViewOrders,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Ver Mis Pedidos")
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isSuccess) "Seguir Comprando" else "Volver")
        }
    }
}
```

---

## PASO 21: SIMPLIFICAR FORMULARIO DE REGISTRO

### Campos Necesarios según Backend

El backend solo requiere estos campos para registrar un cliente:

| Campo | Requerido | Fuente |
|-------|-----------|--------|
| `idUsuario` | Sí | Firebase UID (automático) |
| `email` | Sí | Usuario ingresa |
| `nombreCliente` | Sí | Usuario ingresa |
| `telefonoCliente` | No | Usuario ingresa (opcional) |

### Campos que se Mantienen Locales

| Campo | Razón |
|-------|-------|
| `password` | Manejado por Firebase |
| `profileImageUri` | Persistencia local |
| `gender` | Persistencia local |
| `birthDate` | Persistencia local |
| `street, number, commune, city, region` | Se concatenan para dirección |

### Flujo de Registro Simplificado (Opcional)

Si quieren reducir pasos, pueden combinar Step 1 y Step 2:

```
Step 1: Email, Password, Nombre, Teléfono (opcional)
Step 2: Dirección (con GPS)
Step 3: Foto de perfil (opcional)
Step 4: Género, Fecha nacimiento (opcional) - PUEDE ELIMINARSE
Step 5: Resumen
```

O incluso más simplificado:

```
Step 1: Email, Password, Nombre
Step 2: Dirección (con GPS)
Step 3: Confirmar
```

Los datos opcionales (foto, género, birthDate) se pueden agregar después desde "Editar Perfil".

---

## PASO 22: MANEJO DE ERRORES Y ESTADOS DE CARGA

### Componente reutilizable: `LoadingOverlay.kt`

```kotlin
@Composable
fun LoadingOverlay(
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable(enabled = false) { },
                contentAlignment = Alignment.Center
            ) {
                Card(
                    modifier = Modifier.padding(32.dp),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Cargando...")
                    }
                }
            }
        }
    }
}
```

### Componente: `NetworkErrorView.kt`

```kotlin
@Composable
fun NetworkErrorView(
    message: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Error de Conexión",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = onRetry) {
            Icon(Icons.Default.Refresh, null)
            Spacer(Modifier.width(8.dp))
            Text("Reintentar")
        }
    }
}
```

### Manejo de errores HTTP comunes

```kotlin
// En los repositories, mapear códigos HTTP a mensajes
fun mapHttpError(code: Int, body: String?): String {
    return when (code) {
        400 -> "Datos inválidos: ${body ?: "Verifica la información"}"
        401 -> "Sesión expirada. Inicia sesión nuevamente"
        403 -> "No tienes permiso para esta acción"
        404 -> "Recurso no encontrado"
        409 -> "El recurso ya existe"
        422 -> "Error de validación: ${body ?: ""}"
        500 -> "Error del servidor. Intenta más tarde"
        502, 503, 504 -> "Servidor no disponible. Intenta más tarde"
        else -> "Error desconocido ($code)"
    }
}
```

---

## PASO 23: CONFIGURAR URL BASE PARA PRODUCCIÓN

### Opción 1: BuildConfig con Flavors

```kotlin
// app/build.gradle.kts
android {
    // ...

    buildTypes {
        debug {
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"") // Emulador
            // O usar IP local: "\"http://192.168.1.100:8080/\""
        }
        release {
            buildConfigField("String", "BASE_URL", "\"https://tu-dominio.com/\"")
            isMinifyEnabled = true
            proguardFiles(...)
        }
    }

    // O con flavors
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        create("staging") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"http://TU_IP_VM_ORACLE:8080/\"")
        }
        create("prod") {
            dimension = "environment"
            buildConfigField("String", "BASE_URL", "\"https://api.goldenburgers.com/\"")
        }
    }
}
```

### Opción 2: Archivo de configuración

Crear `app/src/main/assets/config.json`:

```json
{
  "baseUrl": "http://TU_IP_VM:8080/"
}
```

Y leer en código:

```kotlin
object Config {
    lateinit var baseUrl: String

    fun init(context: Context) {
        val json = context.assets.open("config.json")
            .bufferedReader()
            .use { it.readText() }
        val config = Gson().fromJson(json, ConfigData::class.java)
        baseUrl = config.baseUrl
    }
}

data class ConfigData(val baseUrl: String)
```

### Valores según ambiente

| Ambiente | URL Base | Notas |
|----------|----------|-------|
| Desarrollo (Emulador) | `http://10.0.2.2:8080/` | 10.0.2.2 es localhost del host |
| Desarrollo (Dispositivo) | `http://192.168.X.X:8080/` | IP local de tu PC |
| Staging (VM Oracle) | `http://IP_VM_ORACLE:8080/` | IP pública de la VM |
| Producción | `https://api.goldenburgers.com/` | Con HTTPS |

---

## RESUMEN FINAL Y CHECKLIST

### Archivos Creados en esta Parte

| Archivo | Descripción |
|---------|-------------|
| `OrderRepository.kt` | Repository para pedidos y pagos |
| `CheckoutViewModel.kt` | ViewModel del checkout |
| `CheckoutScreen.kt` | Pantalla de checkout |
| `OrderHistoryViewModel.kt` | ViewModel del historial |
| `OrderHistoryScreen.kt` | Pantalla de historial |
| `OrderResultScreen.kt` | Pantalla de resultado de pago |
| `LoadingOverlay.kt` | Componente de carga |
| `NetworkErrorView.kt` | Componente de error de red |

### Checklist de Integración Completa

#### Fase 1: Configuración Base
- [ ] Agregar dependencias (Firebase, Retrofit, Gson)
- [ ] Agregar `google-services.json`
- [ ] Configurar permisos de Internet
- [ ] Crear `ApiClient.kt`
- [ ] Crear `ApiService.kt`
- [ ] Crear `AuthInterceptor.kt`
- [ ] Crear DTOs de request
- [ ] Crear DTOs de response

#### Fase 2: Autenticación
- [ ] Crear `AuthRepository.kt`
- [ ] Actualizar `RegisterViewModel.kt`
- [ ] Actualizar `LoginViewModel.kt`
- [ ] Actualizar `SessionManager.kt`
- [ ] Probar registro con Firebase + Backend
- [ ] Probar login con Firebase + Backend

#### Fase 3: Catálogo y Favoritos
- [ ] Crear `CatalogRepository.kt`
- [ ] Actualizar `CatalogViewModel.kt`
- [ ] Crear tabla `FavoriteProduct`
- [ ] Crear `FavoriteProductDao`
- [ ] Actualizar `HomeScreen.kt` para cargar de backend
- [ ] Actualizar `FavoritesScreen.kt`

#### Fase 4: Perfil y Direcciones
- [ ] Crear `UserLocal` entity
- [ ] Crear `UserLocalDao`
- [ ] Crear `AddressRepository.kt`
- [ ] Actualizar `ProfileViewModel.kt`
- [ ] Actualizar `EditProfileViewModel.kt`

#### Fase 5: Pedidos y Pagos
- [ ] Crear `OrderRepository.kt`
- [ ] Crear `CheckoutViewModel.kt`
- [ ] Crear `CheckoutScreen.kt`
- [ ] Configurar Deep Links para Mercado Pago
- [ ] Probar flujo de pago completo

#### Fase 6: Historial
- [ ] Crear `OrderHistoryViewModel.kt`
- [ ] Crear `OrderHistoryScreen.kt`
- [ ] Agregar navegación a historial

#### Fase 7: Ajustes Finales
- [ ] Configurar URL base por ambiente
- [ ] Agregar manejo de errores
- [ ] Probar en dispositivo físico
- [ ] Probar con backend en VM Oracle

### Pruebas Recomendadas

1. **Registro**: Crear cuenta nueva → Verificar en Firebase Console → Verificar en BD Oracle
2. **Login**: Iniciar sesión → Verificar token JWT → Verificar datos cargados
3. **Catálogo**: Ver productos → Verificar imágenes de Firebase Storage
4. **Favoritos**: Agregar/quitar favoritos → Cerrar app → Verificar persistencia
5. **Carrito**: Agregar productos → Modificar cantidades → Verificar totales
6. **Checkout**: Crear pedido → Verificar en BD Oracle
7. **Pago**: Completar pago en Mercado Pago → Verificar webhook → Verificar estado
8. **Historial**: Ver pedidos → Verificar detalles → Cancelar pedido pendiente

---

## CONTACTO Y SOPORTE

Para problemas con:
- **Firebase**: [Firebase Console](https://console.firebase.google.com/)
- **Mercado Pago**: [Documentación MP](https://www.mercadopago.com.co/developers/es/docs)
- **Oracle Cloud**: [Oracle Cloud Console](https://cloud.oracle.com/)

---

**Anterior:** [PARTE 2 - Autenticación y Catálogo](./INTEGRACION-PARTE2-AUTENTICACION.md)

**Inicio:** [PARTE 1 - Configuración](./INTEGRACION-PARTE1-CONFIGURACION.md)
