package com.example.goldenburgers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.example.goldenburgers.viewmodel.EditProfileViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class ProfileScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun logoutButton_isDisplayed_onScreenLoad() {
        // Arrange: Preparamos los mocks necesarios
        val mockProfileViewModel = mockk<EditProfileViewModel>(relaxed = true)
        val mockSessionManager = mockk<SessionManager>(relaxed = true)
        val mockThemeManager = mockk<ThemeManager>(relaxed = true)

        // Act: "Inflamos" la UI
        composeTestRule.setContent {
            GolgerBurguerTheme {
                val navController = rememberNavController()
                ProfileScreen(
                    navController = navController,
                    sessionManager = mockSessionManager,
                    themeManager = mockThemeManager,
                    profileViewModel = mockProfileViewModel
                )
            }
        }

        // Assert: Buscamos el botón y verificamos que se muestra
        composeTestRule.onNodeWithTag("logout_button").assertIsDisplayed()
    }
}
