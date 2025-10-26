package com.example.goldenburgers.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goldenburgers.model.Producto
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat

/**
 * Esta es la pantalla principal de mi aplicación, donde muestro el catálogo de productos.
 * Es la primera pestaña que ve el usuario al iniciar sesión.
 */
@Composable
fun HomeScreen(catalogViewModel: CatalogViewModel) {
    // Observo el estado (uiState) del CatalogViewModel. Gracias a `collectAsStateWithLifecycle`,
    // mi pantalla reaccionará a cualquier cambio en la lista de productos (y en el resto del estado)
    // de una forma segura y optimizada para el ciclo de vida.
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    // Uso una `LazyVerticalGrid` para mostrar los productos en una cuadrícula.
    // Es "lazy" (perezosa), lo que significa que solo compone y renderiza los elementos que son
    // visibles en pantalla. Esto es súper eficiente, especialmente para listas largas.
    LazyVerticalGrid(
        columns = GridCells.Fixed(2), // Muestro 2 columnas de productos.
        contentPadding = PaddingValues(16.dp), // Un padding general para la cuadrícula.
        horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio horizontal entre las tarjetas.
        verticalArrangement = Arrangement.spacedBy(16.dp) // Espacio vertical entre las tarjetas.
    ) {
        // La función `items` es el corazón de la LazyGrid. Recorre la lista de productos
        // del `uiState` y, para cada `product`, renderiza un `ProductCard`.
        items(uiState.products) { product ->
            ProductCard(product = product, viewModel = catalogViewModel)
        }
    }
}

/**
 * Este es el Composable que define cómo se ve cada tarjeta de producto individualmente.
 * Lo he creado como una función separada para reutilizarlo en otras partes de la app, como en la
 * pantalla de Favoritos. Esto hace mi código mucho más limpio y mantenible.
 */
@Composable
fun ProductCard(product: Producto, viewModel: CatalogViewModel) {
    val context = LocalContext.current
    val imageResId = remember(product.imagenReferencia) {
        // Aunque la propiedad `imagenReferencia` ya es un ID de recurso, uso `remember`
        // para asegurarme de que no se hagan cálculos innecesarios en cada recomposición.
        product.imagenReferencia
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp), // Le doy una pequeña sombra para que resalte.
        shape = RoundedCornerShape(12.dp) // Bordes redondeados para un look más suave.
    ) {
        Column {
            // Uso un `Box` para la imagen, lo que me permite superponer elementos fácilmente,
            // como el icono de favorito.
            Box(modifier = Modifier.height(150.dp)) {
                Image(
                    painter = painterResource(id = imageResId),
                    contentDescription = product.nombre,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                    contentScale = ContentScale.Crop // `Crop` asegura que la imagen llene el espacio sin deformarse.
                )
                // Este es el botón de favorito, alineado en la esquina superior derecha del Box.
                IconButton(
                    onClick = { viewModel.toggleFavorite(product.id, product.esFavorito) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                    Icon(
                        imageVector = if (product.esFavorito) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (product.esFavorito) Color.Red else Color.White // Cambio el color para dar feedback visual.
                    )
                }
            }
            // Columna para el texto y los botones, debajo de la imagen.
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(product.descripcion, style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween, // Esto empuja el precio y el botón a los extremos.
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(product.precio.toCurrencyFormat(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    // El botón para añadir al carrito, ahora con un icono.
                    Button(onClick = { viewModel.addToCart(product) }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir al carrito")
                    }
                }
            }
        }
    }
}
