package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.R
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.LoginViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    navController: NavController,
    loginViewModel: LoginViewModel
) {
    val uiState by loginViewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val isLoginValid = uiState.email.isNotBlank() && uiState.password.isNotBlank() &&
            uiState.emailError == null && uiState.passwordError == null

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondoinicio),
            contentDescription = "Imagen de fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { Text("Iniciar Sesión", color = Color.White) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Black.copy(alpha = 0.5f)),
                    navigationIcon = { 
                        IconButton(onClick = { if (!uiState.isLoading) navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver", tint = Color.White)
                        }
                    }
                )
            }
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                // [CORREGIDO] Se muestra el indicador de carga o el formulario
                if (uiState.isLoading) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(color = Color.White)
                        Spacer(Modifier.height(16.dp))
                        Text("Iniciando sesión...", color = Color.White)
                    }
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("Inicia sesión con tu perfil", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center, color = Color.White)
                        Spacer(Modifier.height(8.dp))
                        Text("Ingrese sus datos para continuar", style = MaterialTheme.typography.bodyLarge, color = Color.White.copy(alpha = 0.8f), textAlign = TextAlign.Center)
                        Spacer(Modifier.height(48.dp))

                        OutlinedTextField(
                            value = uiState.email,
                            onValueChange = { loginViewModel.onEmailChange(it) },
                            label = { Text("Correo electrónico") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            isError = uiState.emailError != null,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.White, focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        AnimatedVisibility(visible = uiState.emailError != null) {
                            Text(uiState.emailError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                        }
                        Spacer(Modifier.height(16.dp))

                        OutlinedTextField(
                            value = uiState.password,
                            onValueChange = { loginViewModel.onPasswordChange(it) },
                            label = { Text("Contraseña") },
                            modifier = Modifier.fillMaxWidth(),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                            singleLine = true,
                            isError = uiState.passwordError != null,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = Color.White, focusedLabelColor = Color.White, unfocusedLabelColor = Color.White.copy(alpha = 0.7f), focusedTextColor = Color.White, unfocusedTextColor = Color.White)
                        )
                        AnimatedVisibility(visible = uiState.passwordError != null) {
                            Text(uiState.passwordError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                        }
                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = {
                                loginViewModel.login(
                                    onSuccess = {
                                        navController.navigate("main_flow") {
                                            popUpTo(AppScreens.WelcomeScreen.route) { inclusive = true }
                                        }
                                    },
                                    onError = { errorMessage ->
                                        Toast.makeText(context, errorMessage, Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            enabled = isLoginValid
                        ) {
                            Text("Ingresar", style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.height(24.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("¿No tienes una cuenta?", style = MaterialTheme.typography.bodyMedium, color = Color.White)
                            Spacer(Modifier.width(4.dp))
                            TextButton(onClick = { navController.navigate(AppScreens.RegisterStep1Screen.route) }) {
                                Text("Registrar", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }
    }
}