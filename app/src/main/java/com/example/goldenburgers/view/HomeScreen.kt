package com.example.goldenburgers.view

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.example.goldenburgers.R
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(catalogViewModel: CatalogViewModel) {
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()
    val categories = listOf("Burgers", "Acompañamientos", "Refrescos", "Kids")
    var selectedCategory by remember { mutableStateOf("Burgers") }
    var selectedProduct by remember { mutableStateOf<Producto?>(null) }

    val displayedProducts = remember(uiState.products, selectedCategory) {
        uiState.products.filter { it.categoria.equals(selectedCategory, ignoreCase = true) }
    }

    Box(modifier = Modifier.fillMaxSize()) { // Usamos un Box para poder mostrar el Dialog por encima
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
        ) {
            BrandHeader()
            CategoryHeader(
                categories = categories,
                selectedCategory = selectedCategory,
                onCategorySelected = { newCategory -> selectedCategory = newCategory }
            )
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(displayedProducts) { product ->
                    ProductCard(product = product, viewModel = catalogViewModel, onProductClick = { selectedProduct = it })
                }
            }
        }

        // Si hay un producto seleccionado, mostramos el Dialog
        selectedProduct?.let {
            ProductDetailDialog(product = it, onDismiss = { selectedProduct = null })
        }
    }
}

@Composable
fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black)
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("GOLDEN", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color.White)
        Text("BURGER", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Black, color = Color(0xFFFFC107))
    }
}

@Composable
fun CategoryHeader(categories: List<String>, selectedCategory: String, onCategorySelected: (String) -> Unit) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().background(Color.Black).padding(bottom = 12.dp),
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) {
            val isSelected = it == selectedCategory
            Box(
                modifier = Modifier.clip(CircleShape).background(if (isSelected) Color(0xFFFFC107) else Color.DarkGray).clickable { onCategorySelected(it) }.padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(it, color = if (isSelected) Color.Black else Color.White, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
fun ProductCard(product: Producto, viewModel: CatalogViewModel, onProductClick: (Producto) -> Unit) {
    var isAdded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val scale by animateFloatAsState(if (isAdded) 1.2f else 1f, tween(300), label = "")
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite = uiState.favorites.any { it.idProducto == product.idProducto }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { onProductClick(product) }, // Hacemos la tarjeta clickeable
        elevation = CardDefaults.cardElevation(4.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Box(modifier = Modifier.height(150.dp)) {
                if (product.imagenUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imagenUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.goldencarga)
                            .error(R.drawable.goldencarga)
                            .memoryCachePolicy(CachePolicy.DISABLED)
                            .diskCachePolicy(CachePolicy.DISABLED)
                            .build(),
                        contentDescription = product.nombreProducto,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(painter = painterResource(id = R.drawable.goldencarga), contentDescription = product.nombreProducto, modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)), contentScale = ContentScale.Crop)
                }
                IconButton(onClick = { viewModel.toggleFavorite(product.idProducto, isFavorite) }, modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)) {
                    Icon(imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder, "Favorito", tint = if (isFavorite) Color.Red else Color.White)
                }
            }
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.nombreProducto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(product.descripcion ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(product.precioBase.toCurrencyFormat(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Button(onClick = { 
                        viewModel.addToCart(product)
                        scope.launch { isAdded = true; delay(200); isAdded = false }
                     }, modifier = Modifier.scale(scale)) {
                        Icon(Icons.Default.Add, "Añadir al carrito")
                    }
                }
            }
        }
    }
}

@Composable
fun ProductDetailDialog(product: Producto, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Card(shape = RoundedCornerShape(16.dp)) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(product.nombreProducto, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(16.dp))
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(product.imagenUrl)
                        .crossfade(true)
                        .placeholder(R.drawable.goldencarga)
                        .error(R.drawable.goldencarga)
                        .memoryCachePolicy(CachePolicy.DISABLED)
                        .diskCachePolicy(CachePolicy.DISABLED)
                        .build(),
                    contentDescription = product.nombreProducto,
                    modifier = Modifier.height(200.dp).fillMaxWidth().clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
                Spacer(Modifier.height(16.dp))
                Text("Descripción", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.heightIn(max = 150.dp)) { // Limitar la altura de la descripción
                    Text(product.descripcion ?: "Sin descripción.", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.verticalScroll(rememberScrollState()))
                }
                Spacer(Modifier.height(24.dp))
                Button(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                    Text("Cerrar")
                }
            }
        }
    }
}