package com.example.goldenburgers

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppNavigation
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteNetworkSource
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.ui.theme.GolgerBurguerTheme
import com.google.firebase.FirebaseApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Creación de instancias
        val themeManager = ThemeManager(this)
        val sessionManager = SessionManager(this)
        val authRepository = AuthRepository()

        // [CORREGIDO] Se crea e inyecta la nueva dependencia de red
        val clienteNetworkSource = ClienteNetworkSource(sessionManager)
        val clienteRepository = ClienteRepository(clienteNetworkSource)

        setContent {
            val isDarkMode by themeManager.isDarkMode.collectAsState(initial = false)
            GolgerBurguerTheme(darkTheme = isDarkMode) {
                AppNavigation(this, sessionManager, themeManager, authRepository, clienteRepository)
            }
        }
    }
}
