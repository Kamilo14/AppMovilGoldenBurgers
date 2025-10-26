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
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.view.EditProfileScreen
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.view.*
import com.example.goldenburgers.viewmodel.*

/**
 * Este es el Composable principal
 * Define todas las pantallas disponibles y gestiona la transición entre ellas.
 * También es el responsable de decidir cuál es la primera pantalla que se debe mostrar.
 */
@Composable
fun AppNavigation(
    // Recibe los gestores de sesión y tema desde MainActivity para que su estado
    // persista durante toda la vida de la aplicación.
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    // Crea el NavController principal para todas las operaciones de navegación.
    val navController = rememberNavController()

    // --- Creación de ViewModels ---
    // Creación de las instancias de ViewModels, en un nivel alto del árbol de Composable.
    // Se utilizan sus respectivas Factories para inyectar las dependencias que necesitan (como el repositorio).
    val loginViewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(catalogViewModel.repository))
    val registerViewModel: RegisterViewModel = viewModel(factory = RegisterViewModelFactory(catalogViewModel.repository))
    val editProfileViewModel: EditProfileViewModel = viewModel(factory = EditProfileViewModelFactory(catalogViewModel.repository, sessionManager))

    // --- Lógica de Arranque ---
    // Observa el Flow del SessionManager para saber si hay un usuario logueado.
    // Se usa un String vacío como estado inicial para poder diferenciar entre tres estados:
    // 1. "" (vacío): La app está cargando y aún no sabemos si hay sesión.
    // 2. null: La carga terminó y se confirmó que NO hay sesión.
    // 3. "user@email.com": La carga terminó y SÍ hay un usuario logueado.
    val loggedInUserEmail by sessionManager.loggedInUserEmailFlow.collectAsState(initial = "")

    // Mientras el estado sea el inicial (String vacío), muestra un indicador de carga.
    val isLoadingSession = loggedInUserEmail == ""
    if (isLoadingSession) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        return // No dibuja el resto de la app hasta que la sesión se haya resuelto.
    }

    // Una vez resuelta la sesión, se decide cuál será la primera pantalla que verá el usuario.
    val startDestination = if (!loggedInUserEmail.isNullOrBlank()) {
        "main_flow" // Si hay un usuario, va directo al flujo principal de la app.
    } else {
        AppScreens.WelcomeScreen.route // Si no, va a la pantalla de bienvenida.
    }

    // --- Grafo de Navegación ---
    // NavHost es el contenedor que alojará todas las pantallas de la aplicación.
    NavHost(navController = navController, startDestination = startDestination) {
        // Se define unas animaciones de transición estándar para que el cambio entre pantallas sea fluido.
        val slideDuration = 300
        val slideIn = slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(slideDuration))
        val slideOut = slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(slideDuration))
        val popIn = slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(slideDuration))
        val popOut = slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(slideDuration))

        // SE define cada una de las pantallas como un destino de navegación.
        composable(AppScreens.WelcomeScreen.route) { WelcomeScreen(navController) }
        composable(AppScreens.LoginScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
            LoginScreen(navController, sessionManager, loginViewModel)
        }

        // --- Flujo de Registro Multi-paso ---
        composable(AppScreens.RegisterStep1Screen.route) { RegisterStep1Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep2Screen.route) { RegisterStep2Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep3Screen.route) { RegisterStep3Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep4Screen.route) { RegisterStep4Screen(navController, registerViewModel) }
        composable(AppScreens.RegisterStep5Screen.route) { RegisterStep5Screen(navController, registerViewModel, sessionManager) }

        // --- Pantallas de Usuario ---
        composable(AppScreens.EditProfileScreen.route, enterTransition = { slideIn }, exitTransition = { slideOut }) {
            EditProfileScreen(navController = navController, viewModel = editProfileViewModel)
        }

        // --- Flujo Principal de la App (Post-Login) ---
        // Este es un destino especial que lanza la MainScreen, la cual contiene la barra de navegación inferior.
        composable("main_flow", enterTransition = { fadeIn(animationSpec = tween(500)) }) {
            MainScreen(mainNavController = navController, sessionManager = sessionManager, themeManager = themeManager, catalogViewModel = catalogViewModel)
        }
    }
}
