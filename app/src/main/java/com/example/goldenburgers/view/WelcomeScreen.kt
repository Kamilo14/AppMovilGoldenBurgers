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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.goldenburgers.R
import com.example.goldenburgers.navigation.AppScreens

@Composable
fun WelcomeScreen(navController: NavController) {

    Box(modifier = Modifier.fillMaxSize()) {
        Image(
            painter = painterResource(id = R.drawable.fondoinicio),
            contentDescription = "Imagen de fondo",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 48.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {

            // [CORREGIDO] Se usa buildAnnotatedString para aplicar estilos diferentes
            Text(
                text = buildAnnotatedString {
                    withStyle(style = SpanStyle(color = Color(0xFFFFD700), fontSize = 82.sp)) {
                        append("G")
                    }
                    append("olden ")
                    withStyle(style = SpanStyle(color = Color(0xFFFFD700), fontSize = 82.sp)) {
                        append("B")
                    }
                    append("urgers")
                },
                style = MaterialTheme.typography.displayLarge,
                color = Color.White, // Color por defecto para el resto del texto
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            /*
            Text(
                text = "El sabor que te hace volver",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center
            )
            */
            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { navController.navigate(AppScreens.LoginScreen.route) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Text("Iniciar Sesión")
            }

            Spacer(modifier = Modifier.height(16.dp))

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
}