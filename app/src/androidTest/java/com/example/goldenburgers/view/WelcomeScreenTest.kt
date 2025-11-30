package com.example.goldenburgers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.ui.theme.GoldenBurgersTheme
import org.junit.Rule
import org.junit.Test

/**
 * Test instrumentado para la WelcomeScreen.
 * Este test se ejecuta en un emulador o dispositivo real.
 */
class WelcomeScreenTest {

    // Regla para testear componentes de Compose
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun elBotonDeLoginDeberiaMostrarseEnLaPantalla() {
        // Arrange: Preparamos el entorno del test
        composeTestRule.setContent {
            // Usamos nuestro tema para que el componente se vea correctamente
            GoldenBurgersTheme {
                // Creamos un NavController falso porque el componente lo requiere
                val navController = rememberNavController()
                WelcomeScreen(navController = navController)
            }
        }

        // Act & Assert: Buscamos el nodo con el testTag y verificamos que se muestra
        composeTestRule
            .onNodeWithTag("login_button") // Busca el componente con la etiqueta que pusimos
            .assertIsDisplayed() // Verifica que el componente es visible
    }
}