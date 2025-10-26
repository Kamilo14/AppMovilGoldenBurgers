package com.example.goldenburgers.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.RegisterViewModel

/**
 * Esta es la primera pantalla del flujo de registro.
 * Su objetivo es recoger las credenciales de acceso del nuevo usuario: email y contraseña.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep1Screen(navController: NavController, viewModel: RegisterViewModel) {

    // Observo el estado (uiState) del RegisterViewModel. La UI se actualizará automáticamente
    // cuando el usuario escriba en los campos o cuando haya errores de validación.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Mi lógica de validación para el botón "Siguiente".
    // Solo se activará si ambos campos tienen texto y no hay ningún error de validación.
    val isStep1Valid = uiState.email.isNotBlank() && uiState.password.isNotBlank() &&
            uiState.emailError == null && uiState.passwordError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Crear Cuenta - Paso 1 de 5") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // La barra de progreso le indica al usuario en qué parte del flujo se encuentra.
                LinearProgressIndicator(
                    progress = { 0.2f }, // 1 de 5 pasos es el 20%
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )

                Text("Crea tus credenciales", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Necesitarás estos datos para iniciar sesión.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(48.dp))

                // --- Campo de Email ---
                // Conecto el valor del campo y la acción de cambio al ViewModel.
                // Esto asegura que la lógica de validación se ejecute en el ViewModel, no en la UI.
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { viewModel.onEmailChange(it) },
                    label = { Text("Correo electrónico") },
                    isError = uiState.emailError != null, // El campo se pone en rojo si hay un error.
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth()
                )
                // El mensaje de error solo es visible si `emailError` no es nulo.
                AnimatedVisibility(visible = uiState.emailError != null) {
                    Text(
                        text = uiState.emailError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // --- Campo de Contraseña ---
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { viewModel.onPasswordChange(it) },
                    label = { Text("Contraseña (mín. 6 caracteres)") },
                    isError = uiState.passwordError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = PasswordVisualTransformation(), // Oculta la contraseña.
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = uiState.passwordError != null) {
                    Text(
                        text = uiState.passwordError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(32.dp))

                // --- Botón de Navegación ---
                Button(
                    onClick = {
                        // Si el formulario es válido, simplemente navego al siguiente paso del registro.
                        if (isStep1Valid) {
                            navController.navigate(AppScreens.RegisterStep2Screen.route)
                        }
                    },
                    enabled = isStep1Valid, // El botón está desactivado si los datos no son válidos.
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Siguiente")
                }

                Spacer(Modifier.height(24.dp))
            }
        }
    }
}
