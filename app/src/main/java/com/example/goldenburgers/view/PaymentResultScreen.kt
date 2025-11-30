package com.example.goldenburgers.view

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun PaymentResultScreen(navController: NavController, wasSuccessful: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        val icon = if (wasSuccessful) Icons.Default.CheckCircle else Icons.Default.Cancel
        val text = if (wasSuccessful) "Pago Aprobado" else "Pago Rechazado"
        val color = if (wasSuccessful) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error

        Icon(
            imageVector = icon,
            contentDescription = text,
            tint = color,
            modifier = Modifier.size(120.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(48.dp))
        Button(
            onClick = { 
                // [CORREGIDO] Navega a la ruta principal del flujo post-login y limpia la pila
                navController.navigate("main_flow") {
                    popUpTo(navController.graph.findStartDestination().id) {
                        inclusive = true
                    }
                    launchSingleTop = true
                }
            },
            modifier = Modifier.fillMaxWidth().height(50.dp)
        ) {
            Text("Finalizar")
        }
    }
}