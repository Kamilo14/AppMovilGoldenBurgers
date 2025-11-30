package com.example.goldenburgers.navigation

/**
 * Se decide usar una `sealed class` (clase sellada) para gestionar todas las rutas de la aplicación.
 */
sealed class AppScreens(val route: String) {

    // --- Pantallas del Flujo de Autenticación y Registro ---
    object WelcomeScreen : AppScreens("welcome_screen")
    object LoginScreen : AppScreens("login_screen")
    object RegisterStep1Screen : AppScreens("register_step1_screen")
    object RegisterStep2Screen : AppScreens("register_step2_screen")
    object RegisterStep3Screen : AppScreens("register_step3_screen")
    object RegisterStep5Screen : AppScreens("register_step5_screen")

    // --- Pantallas de Gestión de Usuario ---
    object EditProfileScreen : AppScreens("edit_profile_screen")
    object AddressListScreen : AppScreens("address_list_screen")
    // [NUEVO] Pantalla para crear/editar una dirección.
    // Acepta un ID opcional. Si no se pasa, es para crear una nueva.
    object EditAddressScreen : AppScreens("edit_address_screen")

    // --- Pantallas Principales (dentro del BottomNav) ---
    object HomeScreen : AppScreens("home_screen")
    object FavoritesScreen : AppScreens("favorites_screen")
    object CartScreen : AppScreens("cart_screen")
    object ProfileScreen : AppScreens("profile_screen")
}
