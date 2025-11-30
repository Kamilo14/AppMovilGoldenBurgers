package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
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
    val context = LocalContext.current

    val deliveryOptions = listOf(
        DeliveryOption(id = 2, title = "Retiro en tienda", description = "Sin costo", cost = 0.0),
        DeliveryOption(id = 1, title = "Delivery", description = "$3.990", cost = 3990.0)
    )
    val paymentOptions = listOf(
        PaymentOption(id = 2, title = "Efectivo"),
        PaymentOption(id = 3, title = "Mercado Pago")
    )

    var selectedDelivery by remember { mutableStateOf<DeliveryOption?>(null) }
    var selectedPayment by remember { mutableStateOf<PaymentOption?>(null) }
    var selectedAddress by remember { mutableStateOf<DireccionCliente?>(null) }
    var showCashConfirmDialog by remember { mutableStateOf(false) }

    val totalPedido = catalogState.cartSubtotal + (selectedDelivery?.cost ?: 0.0)

    LaunchedEffect(Unit) {
        editProfileViewModel.loadCurrentUser()
        addressViewModel.loadAddresses()
    }

    // [CORREGIDO] Lógica para manejar el resultado de la creación de un pedido
    LaunchedEffect(pedidoState.ultimoPedidoCreado) {
        val pedido = pedidoState.ultimoPedidoCreado
        if (pedido != null) {
            // Si se pagó en efectivo, el flujo termina aquí.
            if (pedido.idMetodoPago == 2L) { // 2 = Efectivo
                Toast.makeText(context, "¡Pedido realizado con éxito!", Toast.LENGTH_LONG).show()
                catalogViewModel.clearCart()
                navController.popBackStack()
            } 
            // Si se eligió MercadoPago, navegamos a la pantalla de pago
            else if (pedido.idMetodoPago == 3L) { // 3 = Mercado Pago
                val total = pedido.montoTotal.toFloat()
                navController.navigate("${AppScreens.FakePaymentScreen.route}/${pedido.idPedido}/$total")
            }
            pedidoViewModel.resetUltimoPedido()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Finalizar Pedido") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            // ... (El resto del código de la pantalla no cambia) ...
            // Resumen del Pedido
            item {
                SectionTitle("Resumen del Pedido")
                catalogState.cartItems.forEach {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${it.quantity}x ${it.product.nombreProducto}")
                        Text((it.quantity * it.product.precioBase).toCurrencyFormat())
                    }
                    Spacer(Modifier.height(4.dp))
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Subtotal", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
                    Text(catalogState.cartSubtotal.toCurrencyFormat(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
                }
                 Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Envío", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
                    Text((selectedDelivery?.cost ?: 0.0).toCurrencyFormat(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Normal)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Total", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                    Text(totalPedido.toCurrencyFormat(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(24.dp))
            }

            // Datos de Contacto
            item {
                SectionTitle("Datos de Contacto")
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
            
            // Tipo de Entrega
            item {
                SectionTitle("Tipo de Entrega")
            }
            items(deliveryOptions) {
                SelectableRow(text = it.title, description = it.description, selected = selectedDelivery?.id == it.id) {
                    selectedDelivery = it
                    if (it.id == 2L) selectedAddress = null 
                }
            }
            
            // Selección de Dirección (solo si es Delivery)
            if (selectedDelivery?.id == 1L) {
                 item { 
                     Spacer(Modifier.height(24.dp))
                     SectionTitle("Dirección de Envío") 
                 }
                 if (addressState.isLoading) {
                     item { CircularProgressIndicator() }
                 } else if (addressState.addresses.isEmpty()) {
                     item { Text("No tienes direcciones guardadas.") }
                 } else {
                     items(addressState.addresses) {
                         AddressSelectorCard(it, selectedAddress?.idDireccion == it.idDireccion) { selectedAddress = it }
                         Spacer(Modifier.height(8.dp))
                     }
                 }
                item {
                    Spacer(Modifier.height(16.dp))
                    OutlinedButton(onClick = { navController.navigate(AppScreens.AddressListScreen.route) }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Editar o añadir direcciones")
                    }
                }
            }
            
            // Método de Pago
            item {
                 Spacer(Modifier.height(24.dp))
                 SectionTitle("Método de Pago")
            }
            items(paymentOptions) {
                SelectableRow(text = it.title, description = null, selected = selectedPayment?.id == it.id) { selectedPayment = it }
            }

            // Botón de Confirmar
            item {
                Spacer(Modifier.height(32.dp))
                Button(
                    onClick = {
                         val currentSelectedDelivery = selectedDelivery
                         val currentSelectedPayment = selectedPayment
                         if (currentSelectedDelivery != null && currentSelectedPayment != null) {
                            if (currentSelectedDelivery.id == 1L && selectedAddress == null) {
                                Toast.makeText(context, "Por favor, selecciona una dirección de envío", Toast.LENGTH_SHORT).show()
                            } else {
                                // [CORREGIDO] Simplificamos la lógica. Siempre creamos el pedido.
                                pedidoViewModel.crearPedido(
                                    items = catalogState.cartItems,
                                    subtotal = catalogState.cartSubtotal,
                                    direccion = if (currentSelectedDelivery.id == 1L) selectedAddress else null,
                                    tipoEntrega = currentSelectedDelivery,
                                    metodoPago = currentSelectedPayment
                                )
                            }
                        } else {
                            Toast.makeText(context, "Por favor, completa todos los campos", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !pedidoState.isLoading
                ) {
                    if (pedidoState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    } else {
                        Text("Continuar con el pago")
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



@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun SelectableRow(text: String, description: String?, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text, style = MaterialTheme.typography.bodyLarge)
            if (description != null) {
                Text(description, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}


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

@Composable
private fun InfoRow(label: String, value: String?) {
    if (value != null) {
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
}