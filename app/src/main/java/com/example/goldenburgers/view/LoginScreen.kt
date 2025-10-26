package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.LoginViewModel
import kotlinx.coroutines.launch

/**
 * Esta es la pantalla donde los usuarios existentes pueden iniciar sesión.
 * Como en las otras pantallas, sigo el patrón MVVM, delegando toda la lógica
 * al `LoginViewModel`.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    sessionManager: SessionManager, // Necesito el SessionManager para guardar la sesión si el login es exitoso.
    loginViewModel: LoginViewModel
) {
    // Observo el estado del ViewModel. La UI reaccionará a los cambios en el email, la contraseña y los errores.
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    // `rememberCoroutineScope` me da un alcance para lanzar corutinas de forma segura desde los callbacks de la UI.
    val scope = rememberCoroutineScope()
    // `LocalContext.current` me da el contexto de Android, necesario para mostrar Toasts.
    val context = LocalContext.current

    // Esta es mi lógica de validación para el botón de "Ingresar".
    // El botón solo se activará si ambos campos tienen texto y no hay errores de validación.
    val isLoginValid = uiState.email.isNotBlank() && uiState.password.isNotBlank() &&
            uiState.emailError == null && uiState.passwordError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Iniciar Sesión") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center // Centro el formulario en la pantalla.
            ) {
                Text("Inicia sesión con tu perfil", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Ingrese sus datos para continuar", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Spacer(Modifier.height(48.dp))

                // --- Campo de Email ---
                OutlinedTextField(
                    value = uiState.email,
                    onValueChange = { loginViewModel.onEmailChange(it) }, // Conecto el campo al ViewModel.
                    label = { Text("Correo electrónico") },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    isError = uiState.emailError != null // El campo se pone rojo si hay un error.
                )
                // Muestro el mensaje de error con una animación suave solo si existe.
                AnimatedVisibility(visible = uiState.emailError != null) {
                    Text(uiState.emailError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))

                // --- Campo de Contraseña ---
                OutlinedTextField(
                    value = uiState.password,
                    onValueChange = { loginViewModel.onPasswordChange(it) },
                    label = { Text("Contraseña") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = PasswordVisualTransformation(), // Oculta la contraseña con puntos.
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true,
                    isError = uiState.passwordError != null
                )
                AnimatedVisibility(visible = uiState.passwordError != null) {
                    Text(uiState.passwordError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                }
                Spacer(Modifier.height(32.dp))

                // --- Botón de Login ---
                Button(
                    onClick = {
                        // La UI no hace la lógica, solo le dice al ViewModel: "el usuario quiere iniciar sesión".
                        loginViewModel.login(
                            // El ViewModel me devuelve el resultado a través de estos callbacks.
                            onSuccess = {
                                // Si el login es exitoso, lanzo una corutina para realizar dos acciones:
                                scope.launch {
                                    // 1. Guardo la sesión del usuario usando el SessionManager.
                                    sessionManager.saveUserSession(uiState.email)
                                    // 2. Navego al flujo principal de la app, limpiando el historial anterior.
                                    navController.navigate("main_flow") {
                                        popUpTo(AppScreens.WelcomeScreen.route) { inclusive = true }
                                    }
                                }
                            },
                            onError = { errorMessage ->
                                // Si hay un error, le muestro un Toast al usuario.
                                Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    enabled = isLoginValid // El botón está desactivado si el formulario no es válido.
                ) {
                    Text("Ingresar", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(24.dp))

                // --- Navegación al Registro ---
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("¿No tienes una cuenta?", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.width(4.dp))
                    TextButton(onClick = { navController.navigate(AppScreens.RegisterStep1Screen.route) }) {
                        Text("Registrar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
    }
}
