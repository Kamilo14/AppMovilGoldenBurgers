package com.example.goldenburgers.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.dto.PedidoDTO
import com.example.goldenburgers.viewmodel.PedidoViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat
import java.text.SimpleDateFormat
import java.util.Locale

// [NUEVO] Función para traducir el ID del estado a un texto legible
private fun mapOrderStatus(statusId: Long): String {
    return when (statusId) {
        1L -> "Pendiente de Pago"
        2L -> "Pagado"
        3L -> "Recibido"
        4L -> "En preparación"
        5L -> "En camino"
        6L -> "Entregado"
        7L -> "Cancelado"
        else -> "Desconocido"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    navController: NavController,
    viewModel: PedidoViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.cargarHistorialPedidos()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Pedidos") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> CircularProgressIndicator()
                uiState.error != null -> Text("Error: ${uiState.error}", color = MaterialTheme.colorScheme.error)
                uiState.historialPedidos.isEmpty() -> Text("Aún no tienes pedidos.")
                else -> {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        items(uiState.historialPedidos) { pedido ->
                            OrderHistoryCard(pedido)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderHistoryCard(pedido: PedidoDTO) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val formattedDate = pedido.fechaPedido?.let {
                try {
                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
                    val formatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    formatter.format(parser.parse(it))
                } catch (e: Exception) {
                    "Fecha inválida"
                }
            } ?: "Fecha no disponible"

            InfoRow(label = "Pedido N°", value = pedido.idPedido?.toString() ?: "N/A")
            InfoRow(label = "Fecha", value = formattedDate)
            InfoRow(label = "Total", value = pedido.montoTotal.toCurrencyFormat())
            // [CORREGIDO] Se usa la función de traducción para mostrar el nombre del estado
            InfoRow(label = "Estado", value = mapOrderStatus(pedido.idEstadoPedido))
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, fontWeight = FontWeight.Bold)
        Text(value)
    }
}