package com.example.goldenburgers.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.ShoppingCartCheckout
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goldenburgers.viewmodel.CartItem
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat

/**
 * [ACTUALIZADO] Se elimina la TopAppBar individual.
 */
@Composable
fun CartScreen(catalogViewModel: CatalogViewModel) {
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        if (uiState.cartItems.isEmpty()) {
            EmptyCartView()
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(uiState.cartItems, key = { it.product.id }) {
                    cartItem -> CartItemRow(item = cartItem, viewModel = catalogViewModel)
                }
            }
            CartSummary(subtotal = uiState.cartSubtotal)
        }
    }
}

@Composable
fun EmptyCartView() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.ShoppingCartCheckout, null, modifier = Modifier.size(100.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        Spacer(Modifier.height(16.dp))
        Text("Tu carrito está vacío", style = MaterialTheme.typography.headlineSmall)
        Text("Añade productos para verlos aquí.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: CatalogViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(id = item.product.imagenReferencia),
                contentDescription = item.product.nombre,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(12.dp)),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.nombre, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(item.product.precio.toCurrencyFormat(), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
            }
            QuantitySelector(item = item, viewModel = viewModel)
        }
    }
}

@Composable
fun QuantitySelector(item: CartItem, viewModel: CatalogViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = { viewModel.decreaseQuantity(item.product.id) }, modifier = Modifier.size(32.dp)) {
            val icon = if (item.quantity > 1) Icons.Default.Remove else Icons.Default.Delete
            Icon(icon, "Quitar uno")
        }
        Text("${item.quantity}", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(horizontal = 8.dp))
        IconButton(onClick = { viewModel.addToCart(item.product) }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Add, "Añadir uno")
        }
    }
}

@Composable
fun CartSummary(subtotal: Double) {
    Surface(modifier = Modifier.fillMaxWidth(), shadowElevation = 8.dp) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Subtotal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                Text(subtotal.toCurrencyFormat(), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))
            Text("El costo de envío se calculará después.", style = MaterialTheme.typography.bodySmall)
            
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))
            
            Button(onClick = { /* TODO */ }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Continuar con la compra", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

