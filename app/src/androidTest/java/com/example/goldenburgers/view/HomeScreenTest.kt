package com.example.goldenburgers.view

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.example.goldenburgers.viewmodel.CatalogViewModel
import io.mockk.mockk
import org.junit.Rule
import org.junit.Test

class HomeScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun brandHeader_isDisplayed_onScreenLoad() {
        // Arrange: Preparamos un ViewModel falso para la pantalla
        val mockCatalogViewModel = mockk<CatalogViewModel>(relaxed = true)

        // Act: "Inflamos" la UI dentro del entorno de test
        composeTestRule.setContent {
            GolgerBurguerTheme {
                HomeScreen(catalogViewModel = mockCatalogViewModel)
            }
        }

        // Assert: Buscamos el componente por su etiqueta y verificamos que se muestra
        composeTestRule.onNodeWithTag("brand_header").assertIsDisplayed()
    }
}
