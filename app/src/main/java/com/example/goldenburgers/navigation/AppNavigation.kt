package com.example.goldenburgers.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.goldenburgers.view.EditProfileScreen
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.view.*
import com.example.goldenburgers.viewmodel.*

@Composable
fun AppNavigation(
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    authRepository: com.example.goldenburgers.repository.AuthRepository,
    clienteRepository: ClienteRepository
) {
    val navController = rememberNavController()

    // --- Creación de ViewModels ---
    val loginViewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(authRepository, clienteRepository, sessionManager)
    )
    val registerViewModel: RegisterViewModel = viewModel(
        factory = RegisterViewModelFactory(authRepository)
    )
    val editProfileViewModel: EditProfileViewModel = viewModel(
        factory = EditProfileViewModelFactory(authRepository, clienteRepository, sessionManager)
    )
    val productRepository = com.example.goldenburgers.model.ProductRepository(sessionManager)
    val catalogViewModel: CatalogViewModel = viewModel(
        factory = CatalogViewModelFactory(productRepository, sessionManager, clienteRepository)
    )
    val addressViewModel: AddressViewModel = viewModel(
        factory = AddressViewModelFactory(clienteRepository, authRepository)
    )
    val editAddressViewModel: EditAddressViewModel = viewModel(
        factory = EditAddressViewModelFactory(clienteRepository, authRepository)
    )

    // --- Lógica de Arranque ---
    val loggedInUserEmail by sessionManager.loggedInUserEmailFlow.collectAsState(initial = "")

    val isLoadingSession = loggedInUserEmail == ""
    if (isLoadingSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return
    }

    val startDestination = if (!loggedInUserEmail.isNullOrBlank()) "main_flow" else AppScreens.WelcomeScreen.route

    // --- Grafo de Navegación ---
    NavHost(navController = navController, startDestination = startDestination) {
        val slideDuration = 300
        val slideIn = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(slideDuration))
        val slideOut = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(slideDuration))

        composable(AppScreens.WelcomeScreen.route) { WelcomeScreen(navController) }
        composable(AppScreens.LoginScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
            LoginScreen(navController, loginViewModel)
        }

        // Flujo de Registro
        composable(AppScreens.RegisterStep1Screen.route) { RegisterStep1Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep2Screen.route) { RegisterStep2Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep3Screen.route) { RegisterStep3Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep5Screen.route) { RegisterStep5Screen(navController, registerViewModel, sessionManager, clienteRepository) }

        // Pantallas de Usuario
        composable(AppScreens.EditProfileScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
            EditProfileScreen(navController = navController, viewModel = editProfileViewModel)
        }
        composable(AppScreens.AddressListScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
            AddressListScreen(navController = navController, viewModel = addressViewModel)
        }
        composable(
            route = "${AppScreens.EditAddressScreen.route}/{addressId}",
            arguments = listOf(navArgument("addressId") { type = NavType.LongType; defaultValue = -1L })
        ) { backStackEntry ->
            val addressId = backStackEntry.arguments?.getLong("addressId") ?: -1L
            EditAddressScreen(navController = navController, viewModel = editAddressViewModel, addressId = addressId)
        }

        composable("main_flow", enterTransition = { fadeIn(animationSpec = tween(500)) }) {
            MainScreen(mainNavController = navController, sessionManager = sessionManager, themeManager = themeManager, catalogViewModel = catalogViewModel)
        }
    }
}