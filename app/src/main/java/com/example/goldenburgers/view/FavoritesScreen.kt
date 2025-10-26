package com.example.goldenburgers.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.goldenburgers.viewmodel.CatalogViewModel

/**
 * Esta es la pantalla que muestra al usuario todos los productos que ha marcado como favoritos.
 * Es una vista más personalizada del catálogo.
 */
@Composable
fun FavoritesScreen(catalogViewModel: CatalogViewModel) {
    // Al igual que en HomeScreen, observo el estado del CatalogViewModel. De esta forma,
    // si el usuario añade o quita un favorito en otra pantalla, esta se actualizará automáticamente.
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    // He decidido que si la lista de favoritos está vacía, es mejor mostrar un mensaje
    // amigable en lugar de una pantalla en blanco. Esto mejora mucho la experiencia de usuario.
    if (uiState.favorites.isEmpty()) {
        EmptyFavoritesView()
    } else {
        // Si hay favoritos, uso una `LazyVerticalGrid` igual que en el catálogo para mostrarlos.
        // La gran ventaja aquí es la reutilización de componentes: en lugar de reescribir
        // el código de las tarjetas, simplemente llamo al `ProductCard` que ya había creado.
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // La única diferencia con HomeScreen es que aquí recorro la lista `uiState.favorites`
            // en lugar de `uiState.products`.
            items(uiState.favorites) { product ->
                ProductCard(product = product, viewModel = catalogViewModel)
            }
        }
    }
}

/**
 * Este es un Composable que he creado para mostrar un estado vacío de forma clara.
 * Podría reutilizarlo en otras partes de la app si fuera necesario.
 */
@Composable
fun EmptyFavoritesView() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center // Centro todo el contenido en la pantalla.
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.Favorite,
                contentDescription = null, // El icono es puramente decorativo.
                modifier = Modifier.padding(bottom = 16.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) // Un color grisáceo y sutil.
            )
            Text("No tienes favoritos", style = MaterialTheme.typography.headlineSmall)
            Text(
                "¡Añade productos que te encanten para verlos aquí!",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp)
            )
        }
    }
}
