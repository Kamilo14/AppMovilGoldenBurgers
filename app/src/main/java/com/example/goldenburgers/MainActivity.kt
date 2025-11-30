package com.example.goldenburgers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppNavigation
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme


/**
 * La actividad principal y único punto de entrada de la aplicación.
 * Actualizada para usar Firebase Auth y backend API en lugar de Room DB
 */
class MainActivity : ComponentActivity() {

    private val sessionManager by lazy { SessionManager(this) }
    private val themeManager by lazy { ThemeManager(this) }

    // Repositorios para autenticación y gestión de clientes
    private val authRepository by lazy { AuthRepository() }
    private val clienteRepository by lazy { ClienteRepository(sessionManager) } // <-- Aquí se inyecta sessionManager

    // TODO: Implementar ProductRepository desde el backend (GESTIONCATALOGO)
    // Por ahora, el CatalogViewModel está deshabilitado hasta que implementes
    // la integración con el microservicio de catálogo del backend

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
                        authRepository = authRepository,
                        clienteRepository = clienteRepository
                    )
                }
            }
        }
    }
}
