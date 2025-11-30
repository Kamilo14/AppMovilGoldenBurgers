package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.data.DireccionCliente
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.AddressViewModel
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.EditProfileViewModel
import com.example.goldenburgers.viewmodel.PedidoViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    catalogViewModel: CatalogViewModel,
    addressViewModel: AddressViewModel,
    pedidoViewModel: PedidoViewModel,
    editProfileViewModel: EditProfileViewModel
) {
    val catalogState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val addressState by addressViewModel.uiState.collectAsStateWithLifecycle()
    val pedidoState by pedidoViewModel.uiState.collectAsStateWithLifecycle()
    val profileState by editProfileViewModel.uiState.collectAsStateWithLifecycle()
    var selectedAddress by remember { mutableStateOf<DireccionCliente?>(null) }
    val context = LocalContext.current

    // Cargar datos de perfil y direcciones
    LaunchedEffect(Unit) {
        editProfileViewModel.loadCurrentUser()
        addressViewModel.loadAddresses()
    }

    // Navegar hacia atrás si el pedido se crea con éxito
    LaunchedEffect(pedidoState.pedidoCreadoExitosamente) {
        if (pedidoState.pedidoCreadoExitosamente) {
            Toast.makeText(context, "¡Pedido realizado con éxito!", Toast.LENGTH_LONG).show()
            catalogViewModel.clearCart()
            navController.popBackStack()
            pedidoViewModel.resetPedidoCreado()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Finalizar Pedido") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // Resumen del Pedido
            item {
                Text("Resumen del Pedido", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                catalogState.cartItems.forEach {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.quantity}x ${it.product.nombreProducto}")
                        Text((it.quantity * it.product.precioBase).toCurrencyFormat())
                    }
                    Spacer(Modifier.height(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(catalogState.cartSubtotal.toCurrencyFormat(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }

            // [CORREGIDO] Datos del Cliente con etiquetas
            item {
                Text("Datos de Contacto", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        InfoRow(label = "Nombre", value = profileState.nombreCliente)
                        InfoRow(label = "Email", value = profileState.email)
                        InfoRow(label = "Teléfono", value = profileState.telefonoCliente)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = { navController.navigate(AppScreens.EditProfileScreen.route) }) {
                            Icon(Icons.Default.Edit, contentDescription = "Editar datos", modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Editar datos")
                        }
                    }
                }
                Spacer(Modifier.height(24.dp))
            }

            // Selección de Dirección
            item {
                Text("Seleccionar Dirección de Envío", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
            }

            if (addressState.isLoading) {
                item { CircularProgressIndicator() }
            } else if (addressState.addresses.isEmpty()) {
                item { Text("No tienes direcciones guardadas.") }
            } else {
                items(addressState.addresses) { address ->
                    AddressSelectorCard(address, selectedAddress?.idDireccion == address.idDireccion) {
                        selectedAddress = address
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = { navController.navigate(AppScreens.AddressListScreen.route) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Editar o añadir direcciones")
                }
            }

            // Botón de Confirmar
            item {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                        val currentSelectedAddress = selectedAddress
                        if (currentSelectedAddress != null) {
                            pedidoViewModel.crearPedido(
                                items = catalogState.cartItems,
                                total = catalogState.cartSubtotal,
                                direccion = currentSelectedAddress
                            )
                        } else {
                            Toast.makeText(context, "Por favor, selecciona una dirección", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = selectedAddress != null && !pedidoState.isLoading
                ) {
                    if (pedidoState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Confirmar Pedido")
                    }
                }
            }
        }

        pedidoState.error?.let {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Text(it, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

// [CORREGIDO] Tarjeta de dirección con etiquetas
@Composable
fun AddressSelectorCard(address: DireccionCliente, isSelected: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(12.dp)
            ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(address.alias ?: "Dirección", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                InfoRow(label = "Dirección", value = address.direccion)
                InfoRow(label = "Ciudad", value = address.ciudad.nombreCiudad)
            }
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, "Seleccionada", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

// [NUEVO] Componente reutilizable para mostrar información con etiqueta
@Composable
private fun InfoRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}