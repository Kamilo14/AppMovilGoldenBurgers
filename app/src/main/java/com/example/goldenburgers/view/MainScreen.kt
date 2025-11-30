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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    mainNavController: NavController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    val bottomBarNavController = rememberNavController()

    LaunchedEffect(Unit) {
        catalogViewModel.loadUserName()
    }

    Scaffold(
        // [CORREGIDO] Se usa el color de fondo del tema actual
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = { BottomNavigationBar(navController = bottomBarNavController) }
    ) { innerPadding ->
        Box(modifier = Modifier.padding(innerPadding)) {
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

@Composable
fun BottomNavigationBar(navController: NavHostController) {
    val items = listOf(BottomNavItem.Home, BottomNavItem.Favorites, BottomNavItem.Cart, BottomNavItem.Profile)

    NavigationBar(
        // [CORREGIDO] Se usan colores del tema para que se adapten
        containerColor = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                // [CORREGIDO] Se usan colores del tema para los ítems
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onPrimary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                onClick = {
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

@Composable
fun BottomNavGraph(
    mainNavController: NavController,
    bottomBarNavController: NavHostController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    NavHost(
        navController = bottomBarNavController,
        startDestination = BottomNavItem.Home.route,
        enterTransition = { fadeIn(animationSpec = tween(200)) },
        exitTransition = { fadeOut(animationSpec = tween(200)) }
    ) {
        composable(AppScreens.HomeScreen.route) {
            HomeScreen(catalogViewModel = catalogViewModel)
        }
        composable(AppScreens.FavoritesScreen.route) {
            FavoritesScreen(catalogViewModel = catalogViewModel)
        }
        composable(AppScreens.CartScreen.route) {
            CartScreen(catalogViewModel = catalogViewModel) {
                mainNavController.navigate(AppScreens.CheckoutScreen.route)
            }
        }
        composable(AppScreens.ProfileScreen.route) {
            ProfileScreen(
                navController = mainNavController,
                sessionManager = sessionManager,
                themeManager = themeManager,
                catalogViewModel = catalogViewModel
            )
        }
    }
}