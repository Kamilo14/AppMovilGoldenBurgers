package com.example.goldenburgers.navigation

/**
 * Se decide usar una `sealed class` (clase sellada) para gestionar todas las rutas de la aplicación.
 */
sealed class AppScreens(val route: String) {

    // --- Flujo de Autenticación y Registro ---
    object WelcomeScreen : AppScreens("welcome_screen")
    object LoginScreen : AppScreens("login_screen")
    object RegisterStep1Screen : AppScreens("register_step1_screen")
    object RegisterStep2Screen : AppScreens("register_step2_screen")
    object RegisterStep3Screen : AppScreens("register_step3_screen")
    object RegisterStep5Screen : AppScreens("register_step5_screen")

    // --- Gestión de Usuario ---
    object EditProfileScreen : AppScreens("edit_profile_screen")
    object AddressListScreen : AppScreens("address_list_screen")
    object EditAddressScreen : AppScreens("edit_address_screen")
    object OrderHistoryScreen : AppScreens("order_history_screen") // [NUEVO]

    // --- Flujo de Compra ---
    object CheckoutScreen : AppScreens("checkout_screen")
    object FakePaymentScreen : AppScreens("fake_payment_screen")
    object PaymentResultScreen : AppScreens("payment_result_screen")

    // --- Pantallas Principales (BottomNav) ---
    object HomeScreen : AppScreens("home_screen")
    object FavoritesScreen : AppScreens("favorites_screen")
    object CartScreen : AppScreens("cart_screen")
    object ProfileScreen : AppScreens("profile_screen")
}