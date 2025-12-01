package com.example.goldenburgers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.example.goldenburgers.viewmodel.LoginViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emailInput_isDisplayed_onScreenLoad() {
        // Arrange: Preparamos los mocks necesarios para el ViewModel
        val mockAuthRepo = mockk<AuthRepository>(relaxed = true)
        val mockClienteRepo = mockk<ClienteRepository>(relaxed = true)
        val mockSessionManager = mockk<SessionManager>(relaxed = true)
        val loginViewModel = LoginViewModel(mockAuthRepo, mockClienteRepo, mockSessionManager)

        // Act: "Inflamos" la UI dentro del entorno de test
        composeTestRule.setContent {
            GolgerBurguerTheme {
                // Usamos un NavController de prueba que no hace nada
                val navController = rememberNavController()
                LoginScreen(navController = navController, viewModel = loginViewModel)
            }
        }

        // Assert: Buscamos el componente por su etiqueta y verificamos que se muestra
        composeTestRule.onNodeWithTag("email_input").assertIsDisplayed()
    }
}
