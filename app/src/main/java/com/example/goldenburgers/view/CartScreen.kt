package com.example.goldenburgers.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.goldenburgers.R
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat
import com.example.goldenburgers.viewmodel.CartItem


@Composable
fun CartScreen(catalogViewModel: CatalogViewModel, onCheckoutClick: () -> Unit) {
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Mi Carrito", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(16.dp))

        if (uiState.cartItems.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tu carrito está vacío", style = MaterialTheme.typography.bodyLarge)
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(uiState.cartItems) { item ->
                    CartItemRow(item = item, viewModel = catalogViewModel)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            // [CORREGIDO] Se usa HorizontalDivider en lugar del obsoleto Divider
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Total:", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text(uiState.cartSubtotal.toCurrencyFormat(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onCheckoutClick,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Ir a Pagar")
            }
        }
    }
}

@Composable
fun CartItemRow(item: CartItem, viewModel: CatalogViewModel) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Imagen
            if (item.product.imagenUrl != null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(item.product.imagenUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.goldencarga)
                        .error(R.drawable.goldencarga)
                        .build(),
                    contentDescription = item.product.nombreProducto,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Image(
                    painter = painterResource(id = R.drawable.goldencarga),
                    contentDescription = item.product.nombreProducto,
                    modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
            
            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(item.product.nombreProducto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(item.product.precioBase.toCurrencyFormat(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { viewModel.decreaseQuantity(item.product.idProducto) }) {
                    Icon(Icons.Default.Remove, contentDescription = "Disminuir cantidad")
                }
                Text(item.quantity.toString(), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                IconButton(onClick = { viewModel.increaseQuantity(item.product.idProducto) }) {
                    Icon(Icons.Default.Add, contentDescription = "Aumentar cantidad")
                }
            }
        }
    }
}
