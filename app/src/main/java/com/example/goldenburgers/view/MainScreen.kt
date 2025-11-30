package com.example.goldenburgers.view

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.navigation.BottomNavItem
import com.example.goldenburgers.viewmodel.CatalogViewModel

/**
 * Esta es la pantalla principal que actúa como el "esqueleto" de mi aplicación una vez
 * que el usuario ha iniciado sesión. Su responsabilidad es alojar la barra de navegación
 * inferior y el contenido de cada una de las pestañas principales.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    // Recibo el NavController principal para poder navegar a pantallas que están
    // fuera de este flujo (como la pantalla de bienvenida al cerrar sesión).
    mainNavController: NavController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    // Creo un NavController específico para la barra de navegación inferior.
    // Esto me permite tener una navegación independiente para las pestañas.
    val bottomBarNavController = rememberNavController()
    val uiState by catalogViewModel.uiState.collectAsStateWithLifecycle()

    // Con LaunchedEffect(Unit), esta corrutina se ejecutará una vez cuando
    // MainScreen entre en la composición. Llamamos a loadUserName() para
    // asegurar que la información del usuario esté actualizada.
    LaunchedEffect(Unit) {
        catalogViewModel.loadUserName()
    }

    // Scaffold es el componente de Material 3 que me da la estructura básica de una pantalla
    // (barra superior, barra inferior, contenido, etc.).
    Scaffold(
        // HE ELIMINADO LA TOP APP BAR AQUÍ
        // Porque ahora usamos el BrandHeader en HomeScreen y queremos fondo negro continuo.
        
        // Establecemos el color del contenedor del Scaffold a negro
        containerColor = Color.Black,
        
        // Aquí le paso el Composable de mi barra de navegación inferior.
        bottomBar = { BottomNavigationBar(navController = bottomBarNavController) }
    ) { innerPadding ->
        // El contenido del Scaffold. El `innerPadding` es muy importante para asegurar
        // que mi contenido no se dibuje debajo de las barras superior o inferior.
        Box(modifier = Modifier.padding(innerPadding)) {
            // Aquí llamo al grafo de navegación del BottomNav, que es el que realmente
            // decidirá qué pantalla (HomeScreen, FavoritesScreen, etc.) se debe mostrar.
            BottomNavGraph(
                mainNavController = mainNavController,
                bottomBarNavController = bottomBarNavController,
                sessionManager = sessionManager,
                themeManager = themeManager,
                catalogViewModel = catalogViewModel
            )
        }
    }
}

/**
 * Este Composable define mi barra de navegación inferior.
 */
@Composable
fun BottomNavigationBar(navController: NavHostController) {
    // Esta es la lista de los items que aparecerán en la barra.
    // La he definido en `BottomNavItem.kt` para mantener el código organizado.
    val items = listOf(BottomNavItem.Home, BottomNavItem.Favorites, BottomNavItem.Cart, BottomNavItem.Profile)

    NavigationBar(
        containerColor = Color.Black, // Fondo negro para el navbar
        contentColor = Color.White // Iconos y texto blancos por defecto
    ) {
        // `currentBackStackEntryAsState` es una función muy útil que me permite saber
        // cuál es la pantalla que se está mostrando actualmente.
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        // Recorro la lista de items y creo un `NavigationBarItem` para cada uno.
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route, // El item se marca como seleccionado si su ruta es la actual.
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black, // Icono negro cuando está seleccionado
                    selectedTextColor = Color(0xFFFFC107), // Texto amarillo cuando está seleccionado
                    indicatorColor = Color(0xFFFFC107), // Fondo amarillo del indicador cuando está seleccionado
                    unselectedIconColor = Color.White, // Icono blanco cuando no
                    unselectedTextColor = Color.White // Texto blanco cuando no
                ),
                onClick = {
                    // Lógica de navegación. Cuando el usuario pulsa un item, navego a su ruta.
                    // Las opciones `popUpTo`, `launchSingleTop` y `restoreState` son cruciales
                    // para un comportamiento correcto de la barra de navegación, evitando que se
                    // acumulen copias de la misma pantalla en el historial.
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}

/**
 * Este es el grafo de navegación para las pantallas del BottomNav.
 * Es un `NavHost` anidado que vive dentro de la MainScreen.
 */
@Composable
fun BottomNavGraph(
    mainNavController: NavController,
    bottomBarNavController: NavHostController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    NavHost(
        navController = bottomBarNavController, // Usa el NavController del BottomNav.
        startDestination = BottomNavItem.Home.route, // La primera pantalla que se muestra es el Home.
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        // Aquí defino cada una de las pantallas de las pestañas.
        composable(AppScreens.HomeScreen.route) {
            HomeScreen(catalogViewModel = catalogViewModel)
        }
        composable(AppScreens.FavoritesScreen.route) {
            FavoritesScreen(catalogViewModel = catalogViewModel)
        }
        composable(AppScreens.CartScreen.route) {
            // Le pasamos un callback vacío para onCheckoutClick, o implementamos una acción
            // como navegar a una pantalla de Checkout futura.
            // Por ahora, solo un print o lambda vacía para que compile.
            CartScreen(catalogViewModel = catalogViewModel, onCheckoutClick = {
                // TODO: Implementar navegación a pantalla de pago
                println("Ir a pagar")
            })
        }
        composable(AppScreens.ProfileScreen.route) {
            // A la pantalla de perfil le paso todas las dependencias que necesita.
            ProfileScreen(
                navController = mainNavController,
                sessionManager = sessionManager,
                themeManager = themeManager,
                catalogViewModel = catalogViewModel
            )
        }
    }
}
