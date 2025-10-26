package com.example.goldenburgers.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.goldenburgers.R
import com.example.goldenburgers.navigation.AppScreens

/**
 * Esta es la primera pantalla que ve un usuario nuevo.
 * Su único propósito es presentar la marca y ofrecer las dos acciones principales:
 * Iniciar Sesión o Registrarse.
 */
@Composable
fun WelcomeScreen(navController: NavController) {

    // He usado una Columna para organizar los elementos verticalmente.
    // El `horizontalAlignment` en `CenterHorizontally` asegura que todo esté centrado.
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        // Título principal de la App.
        // Uso los estilos y colores definidos en mi archivo Theme.kt para mantener la consistencia.
        Text(
            text = "Golden Burgers",
            style = MaterialTheme.typography.displayLarge,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp)) // Un pequeño espacio.

        // Eslogan de la App.
        Text(
            text = "El sabor que te hace volver",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.secondary,
            textAlign = TextAlign.Center
        )

        // Empujo el contenido hacia abajo usando un Spacer con `weight`.
        // Esto hace que la imagen y los botones queden en la parte inferior de la pantalla.
        Spacer(modifier = Modifier.weight(1f))

        // Imagen principal de la marca.
        Image(
            painter = painterResource(id = R.drawable.logo), // logo en drawables.
            contentDescription = "Logo de Golden Burgers",
            modifier = Modifier.fillMaxWidth(0.7f) // La imagen ocupa el 70% del ancho.
        )

        Spacer(modifier = Modifier.weight(1f))

        // Botón principal para iniciar sesión.
        Button(
            onClick = { navController.navigate(AppScreens.LoginScreen.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Iniciar Sesión")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Botón secundario (delineado) para registrarse.
        OutlinedButton(
            onClick = { navController.navigate(AppScreens.RegisterStep1Screen.route) },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Registrarse")
        }
    }
}
