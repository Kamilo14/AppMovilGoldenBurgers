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
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.goldenburgers.R
import com.example.goldenburgers.model.data.Producto
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.toCurrencyFormat
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Esta es la pantalla principal de mi aplicación, donde muestro el catálogo de productos.
 * Es la primera pestaña que ve el usuario al iniciar sesión.
 */
@Composable
fun HomeScreen(catalogViewModel: CatalogViewModel) {
    // Observo el estado (uiState) del CatalogViewModel.
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    // --- ESTADO PARA EL FILTRO DE CATEGORÍAS ---
    // Definimos las categorías exactas que quieres mostrar
    val categories = listOf("Burgers", "Acompañamientos", "Refrescos", "Kids")
    // Estado local para saber qué categoría está seleccionada. Por defecto "Burgers"
    var selectedCategory by remember { mutableStateOf("Burgers") }

    // --- LÓGICA DE FILTRADO ---
    // Filtramos la lista de productos basándonos en la categoría seleccionada.
    // Usamos 'remember' para que no se recalcule innecesariamente.
    val displayedProducts = remember(uiState.products, selectedCategory) {
        uiState.products.filter { producto ->
            // Comparamos ignorando mayúsculas/minúsculas para evitar errores de coincidencia
            producto.categoria.equals(selectedCategory, ignoreCase = true)
        }
    }

    Column(modifier = Modifier
        .fillMaxSize()
        .background(Color.Black)) { // Fondo negro para toda la pantalla
        // 0. Encabezado de Marca (Logo GoldenBurger)
        BrandHeader()

        // 1. Encabezado de Categorías (LazyRow para scroll horizontal)
        CategoryHeader(
            categories = categories,
            selectedCategory = selectedCategory,
            onCategorySelected = { newCategory -> selectedCategory = newCategory }
        )

        // 2. Grilla de Productos
        // Es "lazy" (perezosa), lo que significa que solo compone y renderiza los elementos visibles.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2), // Muestro 2 columnas de productos.
            contentPadding = PaddingValues(16.dp), // Un padding general para la cuadrícula.
            horizontalArrangement = Arrangement.spacedBy(16.dp), // Espacio horizontal entre las tarjetas.
            verticalArrangement = Arrangement.spacedBy(16.dp), // Espacio vertical entre las tarjetas.
            modifier = Modifier.weight(1f) // Ocupa el resto del espacio disponible
        ) {
            // Usamos la lista filtrada 'displayedProducts' en lugar de todos los productos
            items(displayedProducts) { product ->
                ProductCard(product = product, viewModel = catalogViewModel)
            }
        }
    }
}

/**
 * Componente para mostrar el logo de la marca como encabezado
 */
@Composable
fun BrandHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black) // Fondo negro
            .padding(top = 16.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "GOLDEN",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color.White
        )
        Text(
            text = "BURGER",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Black,
            color = Color(0xFFFFC107) // Amarillo Dorado
        )
    }
}

/**
 * Componente para mostrar la barra de categorías horizontal
 */
@Composable
fun CategoryHeader(
    categories: List<String>,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {//Contenedor de Categorias
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Black) // Fondo negro como en tu imagen
            .padding(bottom = 12.dp), // Reduje el padding vertical superior porque ya está el logo
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(categories) { category ->
            val isSelected = category == selectedCategory
            
            // El color amarillo dorado para el fondo del botón
            val goldenColor = Color(0xFFFFC107) 

            Box(
                modifier = Modifier
                    .clip(CircleShape) // Bordes completamente redondeados
                    .background(if (isSelected) goldenColor else Color.DarkGray) // Amarillo si seleccionado, gris si no
                    .clickable { onCategorySelected(category) }
                    .padding(horizontal = 20.dp, vertical = 10.dp), // Padding interno del botón
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    color = if (isSelected) Color.Black else Color.White, // Texto negro en fondo amarillo
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

/**
 * Este es el Composable que define cómo se ve cada tarjeta de producto individualmente.
 */
@Composable
fun ProductCard(product: Producto, viewModel: CatalogViewModel) {
    // Estado para controlar la animación del botón de añadir al carrito
    var isAdded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    // Animación de escala para el botón de carrito
    val scale by animateFloatAsState(
        targetValue = if (isAdded) 1.2f else 1f,
        animationSpec = tween(durationMillis = 300),
        label = "Cart Animation"
    )

    // Verificamos si el producto actual está en la lista de favoritos del ViewModel
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isFavorite = uiState.favorites.any { it.idProducto == product.idProducto }

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp), // Le doy una pequeña sombra para que resalte.
        shape = RoundedCornerShape(12.dp) // Bordes redondeados para un look más suave.
    ) {
        Column {
            // Uso un `Box` para la imagen, lo que me permite superponer elementos fácilmente
            Box(modifier = Modifier.height(150.dp)) {
                // Si tenemos URL de imagen, usamos AsyncImage (Coil). Si no, un placeholder.
                if (product.imagenUrl != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(product.imagenUrl)
                            .crossfade(true)
                            .placeholder(R.drawable.golden) // Imagen mientras carga
                            .error(R.drawable.golden) // Imagen si falla
                            .build(),
                        contentDescription = product.nombreProducto,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback a recurso local si no hay URL
                    Image(
                        painter = painterResource(id = R.drawable.golden),
                        contentDescription = product.nombreProducto,
                        modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                // Botón de favorito
                IconButton(
                    onClick = { viewModel.toggleFavorite(product.idProducto, isFavorite) },
                    modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
                ) {
                     Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorito",
                        tint = if (isFavorite) Color.Red else Color.White // Rojo si es favorito, blanco si no
                    )
                }
            }
            // Columna para el texto y los botones, debajo de la imagen.
            Column(modifier = Modifier.padding(12.dp)) {
                Text(product.nombreProducto, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(product.descripcion ?: "", style = MaterialTheme.typography.bodySmall, maxLines = 2)
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(product.precioBase.toCurrencyFormat(), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    
                    // El botón para añadir al carrito con animación
                    Button(
                        onClick = { 
                            viewModel.addToCart(product)
                            // Disparar animación
                            scope.launch {
                                isAdded = true
                                delay(200) // Esperar un poco
                                isAdded = false
                            }
                        },
                        modifier = Modifier.scale(scale) // Aplicar escala animada
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir al carrito")
                    }
                }
            }
        }
    }
}
