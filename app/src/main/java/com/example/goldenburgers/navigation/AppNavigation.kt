package com.example.goldenburgers.navigation

import android.content.Context
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
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
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.repository.FavoritesRepository
import com.example.goldenburgers.repository.PedidoRepository
import com.example.goldenburgers.view.*
import com.example.goldenburgers.viewmodel.*

@Composable
fun AppNavigation(
    context: Context, 
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    authRepository: AuthRepository,
    clienteRepository: ClienteRepository
) {
    val navController = rememberNavController()

    // --- Repositorios ---
    val favoritesRepository = FavoritesRepository(context, sessionManager)
    val productRepository = ProductRepository(sessionManager, favoritesRepository)
    val pedidoRepository = PedidoRepository(sessionManager)

    // --- ViewModels ---
    val catalogViewModel: CatalogViewModel = viewModel { CatalogViewModelFactory(productRepository, sessionManager, clienteRepository).create(CatalogViewModel::class.java) }
    val addressViewModel: AddressViewModel = viewModel { AddressViewModelFactory(clienteRepository, authRepository).create(AddressViewModel::class.java) }
    val editAddressViewModel: EditAddressViewModel = viewModel { EditAddressViewModelFactory(clienteRepository, authRepository).create(EditAddressViewModel::class.java) }
    val pedidoViewModel: PedidoViewModel = viewModel { PedidoViewModelFactory(pedidoRepository, clienteRepository).create(PedidoViewModel::class.java) }
    val editProfileViewModel: EditProfileViewModel = viewModel { EditProfileViewModelFactory(authRepository, clienteRepository, sessionManager).create(EditProfileViewModel::class.java) }
    val fakePaymentViewModel: FakePaymentViewModel = viewModel { FakePaymentViewModelFactory(pedidoRepository).create(FakePaymentViewModel::class.java) }


    val loggedInUserEmail by sessionManager.loggedInUserEmailFlow.collectAsState(initial = "")
    val startDestination = if (!loggedInUserEmail.isNullOrBlank()) "main_flow" else AppScreens.WelcomeScreen.route

    if (loggedInUserEmail == "") {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
    } else {
        NavHost(navController = navController, startDestination = startDestination) {
            val slideIn = slideInHorizontally(animationSpec = tween(300)) { it }
            val slideOut = slideOutHorizontally(animationSpec = tween(300)) { -it }
            
            composable(AppScreens.WelcomeScreen.route) { WelcomeScreen(navController) }
            composable(AppScreens.LoginScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
                 val loginViewModel: LoginViewModel = viewModel { LoginViewModelFactory(authRepository, clienteRepository, sessionManager).create(LoginViewModel::class.java) }
                 LoginScreen(navController, loginViewModel)
            }
            composable(AppScreens.RegisterStep1Screen.route) { 
                val registerViewModel: RegisterViewModel = viewModel { RegisterViewModelFactory(authRepository).create(RegisterViewModel::class.java) }
                RegisterStep1Screen(navController, registerViewModel) 
            }
            composable(AppScreens.RegisterStep2Screen.route) { 
                val registerViewModel: RegisterViewModel = viewModel { RegisterViewModelFactory(authRepository).create(RegisterViewModel::class.java) }
                RegisterStep2Screen(navController, registerViewModel) 
            }
            composable(AppScreens.RegisterStep3Screen.route) { 
                val registerViewModel: RegisterViewModel = viewModel { RegisterViewModelFactory(authRepository).create(RegisterViewModel::class.java) }
                RegisterStep3Screen(navController, registerViewModel) 
            }
            composable(AppScreens.RegisterStep5Screen.route) { 
                val registerViewModel: RegisterViewModel = viewModel { RegisterViewModelFactory(authRepository).create(RegisterViewModel::class.java) }
                RegisterStep5Screen(navController, registerViewModel, sessionManager, clienteRepository) 
            }
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
            composable(AppScreens.CheckoutScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
                CheckoutScreen(navController, catalogViewModel, addressViewModel, pedidoViewModel, editProfileViewModel)
            }

            // [CORREGIDO] Se pasa el catalogViewModel a FakePaymentScreen
            composable(
                route = "${AppScreens.FakePaymentScreen.route}/{orderId}/{totalAmount}",
                arguments = listOf(
                    navArgument("orderId") { type = NavType.LongType },
                    navArgument("totalAmount") { type = NavType.FloatType }
                )
            ) { backStackEntry ->
                val orderId = backStackEntry.arguments?.getLong("orderId") ?: -1L
                val totalAmount = backStackEntry.arguments?.getFloat("totalAmount")?.toDouble() ?: 0.0
                FakePaymentScreen(navController, fakePaymentViewModel, catalogViewModel, orderId, totalAmount)
            }
            composable(
                route = "${AppScreens.PaymentResultScreen.route}/{wasSuccessful}",
                arguments = listOf(navArgument("wasSuccessful") { type = NavType.BoolType })
            ) { backStackEntry ->
                val wasSuccessful = backStackEntry.arguments?.getBoolean("wasSuccessful") ?: false
                PaymentResultScreen(navController, wasSuccessful)
            }

            composable("main_flow", enterTransition = { fadeIn(animationSpec = tween(500)) }) {
                MainScreen(mainNavController = navController, sessionManager = sessionManager, themeManager = themeManager, catalogViewModel = catalogViewModel)
            }
        }
    }
}