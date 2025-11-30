package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.CiudadesDisponibles
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.repository.ClienteRepository
import com.example.goldenburgers.viewmodel.RegisterViewModel

/**
 * [CORREGIDO] La pantalla final de registro. Ahora solo notifica al ViewModel
 * y no contiene lógica de sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep5Screen(
    navController: NavController,
    viewModel: RegisterViewModel,
    sessionManager: SessionManager,
    clienteRepository: ClienteRepository
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Resumen de Registro") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = { 1.0f }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

                Text("¡Todo listo!", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Verifica la información antes de finalizar.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                Spacer(Modifier.height(32.dp))

                LazyColumn(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    item { SummarySectionTitle("1. Acceso y Contacto") }
                    item { SummaryItem("Email", uiState.email) }
                    item { SummaryItem("Contraseña", "********") }
                    item { HorizontalDivider(Modifier.padding(vertical = 12.dp)) }
                    item { SummaryItem("Nombre Completo", uiState.fullName) }
                    item { SummaryItem("Teléfono", uiState.phoneNumber.ifBlank { "No especificado" }) }
                    item { HorizontalDivider(Modifier.padding(vertical = 24.dp)) }

                    item { SummarySectionTitle("2. Dirección de Despacho") }
                    item { SummaryItem("Ciudad", uiState.idCiudad?.let { CiudadesDisponibles.obtenerNombrePorId(it) } ?: "No especificada") }
                    item { SummaryItem("Dirección", uiState.direccion.ifBlank { "No especificada" }) }
                    item { SummaryItem("Alias", uiState.alias.ifBlank { "No especificado" }) }
                    item { Spacer(Modifier.height(24.dp)) }
                }

                Button(
                    onClick = {
                        // La UI notifica al ViewModel, pasándole las dependencias que necesita.
                        viewModel.onRegisterClicked(
                            clienteRepository = clienteRepository,
                            sessionManager = sessionManager,
                            onSuccess = {
                                // El ViewModel ya se encargó de guardar la sesión. Aquí solo navegamos.
                                navController.navigate("main_flow") {
                                    popUpTo(AppScreens.WelcomeScreen.route) { inclusive = true }
                                }
                            },
                            onError = { errorMessage ->
                                Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                    } else {
                        Text("Confirmar Registro", style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun SummarySectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 4.dp))
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.End, modifier = Modifier.weight(2f))
    }
}
