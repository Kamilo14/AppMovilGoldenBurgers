package com.example.goldenburgers.navigation // Asegúrate que el paquete coincida


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.graphics.vector.ImageVector


/**
 * Sealed class para definir los items del menú de navegación inferior (BottomNavigationBar).
 * Cada objeto representa una pestaña con su título, icono y ruta de navegación.
 */
sealed class BottomNavItem(
    val title: String, // Texto que se muestra debajo del icono
    val icon: ImageVector, // Icono de Material Icons a mostrar
    val route: String // Ruta de navegación asociada (de AppScreens)
) {
    // Objeto para la pestaña "Inicio"
    object Home : BottomNavItem(
        title = "Inicio",
        icon = Icons.Default.Home,
        route = AppScreens.HomeScreen.route // Ruta definida en AppScreens.kt
    )


    // Objeto para la pestaña "Favoritos"
    object Favorites : BottomNavItem(
        title = "Favoritos",
        icon = Icons.Default.Favorite,
        route = AppScreens.FavoritesScreen.route // Ruta definida en AppScreens.kt
    )


    // Objeto para la pestaña "Carrito"
    object Cart : BottomNavItem(
        title = "Carrito",
        icon = Icons.Default.ShoppingCart,
        route = AppScreens.CartScreen.route // Ruta definida en AppScreens.kt
    )


    // Objeto para la pestaña "Perfil"
    object Profile : BottomNavItem(
        title = "Perfil",
        icon = Icons.Default.Person,
        route = AppScreens.ProfileScreen.route // Ruta definida en AppScreens.kt
    )
}






