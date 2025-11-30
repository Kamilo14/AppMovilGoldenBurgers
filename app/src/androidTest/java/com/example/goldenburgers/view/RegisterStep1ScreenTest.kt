package com.example.goldenburgers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.navigation.compose.rememberNavController
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.example.goldenburgers.viewmodel.RegisterViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class RegisterStep1ScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun emailInput_isDisplayed_onScreenLoad() {
        // Arrange
        val mockRegisterViewModel = mockk<RegisterViewModel>(relaxed = true)

        // Act
        composeTestRule.setContent {
            GolgerBurguerTheme {
                val navController = rememberNavController()
                RegisterStep1Screen(navController = navController, viewModel = mockRegisterViewModel)
            }
        }

        // Assert
        composeTestRule.onNodeWithTag("register_email_input").assertIsDisplayed()
    }
}
