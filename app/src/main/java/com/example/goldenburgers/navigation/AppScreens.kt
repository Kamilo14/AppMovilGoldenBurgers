package com.example.goldenburgers.navigation

/**
 * Se decide usar una `sealed class` (clase sellada) para gestionar todas las rutas de la aplicación.
 */
sealed class AppScreens(val route: String) {

    // --- Pantallas del Flujo de Autenticación y Registro ---
    // Estas son las pantallas que ve el usuario antes de iniciar sesión.
    object WelcomeScreen : AppScreens("welcome_screen")
    object LoginScreen : AppScreens("login_screen")
    object RegisterStep1Screen : AppScreens("register_step1_screen")
    object RegisterStep2Screen : AppScreens("register_step2_screen")
    object RegisterStep3Screen : AppScreens("register_step3_screen")
    object RegisterStep4Screen : AppScreens("register_step4_screen")
    object RegisterStep5Screen : AppScreens("register_step5_screen")

    // --- Pantallas de Gestión de Usuario ---
    // Pantallas a las que se accede una vez que el usuario ya está logueado.
    object EditProfileScreen : AppScreens("edit_profile_screen")

    // --- Pantallas Principales (dentro del BottomNav) ---
    // Estas son las rutas para las pestañas de la barra de navegación inferior.
    object HomeScreen : AppScreens("home_screen")
    object FavoritesScreen : AppScreens("favorites_screen")
    object CartScreen : AppScreens("cart_screen")
    object ProfileScreen : AppScreens("profile_screen")
}
