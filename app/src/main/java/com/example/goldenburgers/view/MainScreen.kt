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
        containerColor = Color.Black,
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
        containerColor = Color.Black,
        contentColor = Color.White
    ) {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                label = { Text(text = item.title) },
                icon = { Icon(imageVector = item.icon, contentDescription = item.title) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Black,
                    selectedTextColor = Color(0xFFFFC107),
                    indicatorColor = Color(0xFFFFC107),
                    unselectedIconColor = Color.White,
                    unselectedTextColor = Color.White
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
            CartScreen(catalogViewModel = catalogViewModel, onCheckoutClick = { /* TODO */ })
        }
        composable(AppScreens.ProfileScreen.route) {
            // [CORREGIDO] ProfileScreen ya no necesita el addressViewModel
            ProfileScreen(
                navController = mainNavController,
                sessionManager = sessionManager,
                themeManager = themeManager,
                catalogViewModel = catalogViewModel
            )
        }
    }
}
