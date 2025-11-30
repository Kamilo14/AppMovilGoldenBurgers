package com.example.goldenburgers.view

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.EditProfileViewModel
import kotlinx.coroutines.launch

@Composable
fun ProfileScreen(
    navController: NavController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    profileViewModel: EditProfileViewModel
) {
    val uiState by profileViewModel.uiState.collectAsStateWithLifecycle()
    val isDarkMode by themeManager.isDarkMode.collectAsStateWithLifecycle(initialValue = false)
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        profileViewModel.loadCurrentUser()
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text("Mi Perfil", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (uiState.isLoading) {
                CircularProgressIndicator()
            } else {
                Text(uiState.nombreCliente, style = MaterialTheme.typography.titleLarge)
                Text(uiState.email, style = MaterialTheme.typography.bodyMedium)
            }
            Spacer(Modifier.height(32.dp))
        }

        // [CORRECCIÓN VISUAL] Se agrupan las opciones en una sola Card
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column {
                    ProfileOption(icon = Icons.Default.Edit, text = "Editar Perfil") { navController.navigate(AppScreens.EditProfileScreen.route) }
                    HorizontalDivider()
                    ProfileOption(icon = Icons.Default.LocationOn, text = "Mis Direcciones") { navController.navigate(AppScreens.AddressListScreen.route) }
                    HorizontalDivider()
                    ProfileOption(icon = Icons.AutoMirrored.Filled.ListAlt, text = "Historial de Pedidos") { navController.navigate(AppScreens.OrderHistoryScreen.route) }
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        item {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (isDarkMode) Icons.Default.DarkMode else Icons.Default.WbSunny, contentDescription = "Toggle Dark Mode", modifier = Modifier.padding(end = 16.dp))
                Text("Modo Oscuro", modifier = Modifier.weight(1f))
                Switch(checked = isDarkMode, onCheckedChange = { 
                    scope.launch { themeManager.setDarkMode(!isDarkMode) } 
                })
            }
        }

        item {
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    scope.launch {
                        sessionManager.clearUserSession()
                        navController.navigate(AppScreens.LoginScreen.route) {
                            popUpTo("main_flow") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().testTag("logout_button"),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) {
                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Cerrar Sesión")
            }
        }
    }
}

// [CORRECCIÓN VISUAL] ProfileOption ya no es una Card, sino una fila clickeable
@Composable
private fun ProfileOption(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = text, modifier = Modifier.padding(end = 16.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
    }
}
