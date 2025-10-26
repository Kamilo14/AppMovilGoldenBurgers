package com.example.goldenburgers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import com.example.goldenburgers.model.GolgerBurguerDatabase
import com.example.goldenburgers.model.ProductRepository
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppNavigation
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.example.goldenburgers.viewmodel.CatalogViewModel
import com.example.goldenburgers.viewmodel.CatalogViewModelFactory


/**
 * La actividad principal y único punto de entrada de la aplicación.
 */
class MainActivity : ComponentActivity() {

    private val sessionManager by lazy { SessionManager(this) }
    private val themeManager by lazy { ThemeManager(this) }

    // [ACTUALIZADO] Se le pasa el SessionManager a la factory del CatalogViewModel.
    private val catalogViewModel: CatalogViewModel by viewModels {
        val database = GolgerBurguerDatabase.getDatabase(this)
        val repository = ProductRepository(database.productDao(), database.userDao())
        CatalogViewModelFactory(repository, sessionManager) // <-- AÑADIDO sessionManager
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val isDarkMode by themeManager.isDarkMode.collectAsState(initial = false)

            GolgerBurguerTheme(darkTheme = isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(
                        sessionManager = sessionManager,
                        themeManager = themeManager,
                        catalogViewModel = catalogViewModel
                    )
                }
            }
        }
    }
}
