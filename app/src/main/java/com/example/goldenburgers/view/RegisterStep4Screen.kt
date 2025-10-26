package com.example.goldenburgers.view

import android.icu.text.SimpleDateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.RegisterViewModel
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Locale

/**
 * Esta es la cuarta pantalla del flujo de registro.
 * Aquí recojo datos demográficos opcionales: género y fecha de nacimiento.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep4Screen(navController: NavController, viewModel: RegisterViewModel) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // --- Estados para controlar los menús desplegables ---
    var showDatePicker by remember { mutableStateOf(false) }
    var isGenderMenuExpanded by remember { mutableStateOf(false) }
    val genderOptions = listOf("Masculino", "Femenino", "Otro", "Prefiero no decirlo")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Detalles Opcionales") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(progress = { 0.8f }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            Text("Un poco más sobre ti", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Estos datos nos ayudan a personalizar tu experiencia. (Opcional)", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(48.dp))

            // --- Selector de Género ---
            // He usado un `ExposedDropdownMenuBox` para crear un menú desplegable que sigue el estilo de Material 3.
            ExposedDropdownMenuBox(
                expanded = isGenderMenuExpanded,
                onExpandedChange = { isGenderMenuExpanded = !isGenderMenuExpanded },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = uiState.gender,
                    onValueChange = {}, // No permito que el usuario escriba directamente.
                    readOnly = true,
                    label = { Text("Género") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isGenderMenuExpanded) },
                    modifier = Modifier.menuAnchor().fillMaxWidth()
                )
                ExposedDropdownMenu(expanded = isGenderMenuExpanded, onDismissRequest = { isGenderMenuExpanded = false }) {
                    genderOptions.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                viewModel.onGenderChange(option)
                                isGenderMenuExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // --- Selector de Fecha de Nacimiento ---
            OutlinedTextField(
                value = uiState.birthDate,
                onValueChange = {}, // El campo es de solo lectura.
                readOnly = true,
                label = { Text("Fecha de Nacimiento") },
                // El icono del calendario es el que activa el DatePicker.
                trailingIcon = { Icon(Icons.Default.DateRange, "Abrir calendario", modifier = Modifier.clickable { showDatePicker = true }) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(32.dp))

            // --- Botones de Navegación ---
            Button(onClick = { navController.navigate(AppScreens.RegisterStep5Screen.route) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Siguiente")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(AppScreens.RegisterStep5Screen.route) }) {
                Text("Omitir")
            }
        }
    }

    // --- Diálogo del DatePicker ---
    // El DatePicker solo se muestra si `showDatePicker` es true.
    if (showDatePicker) {
        // Calcula la fecha máxima de selección (hace 18 años). Así me aseguro de que el usuario sea mayor de edad.
        val maxDate = LocalDate.now().minus(18, ChronoUnit.YEARS)
        val maxDateMillis = maxDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = maxDateMillis,
            // El rango de años seleccionable termina en el año de maxDate (ej: 2007).
            yearRange = (LocalDate.now().minus(100, ChronoUnit.YEARS).year..maxDate.year),
        )
        
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        // Formateo la fecha seleccionada al formato AAAA-MM-DD y la guardo en el ViewModel.
                        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        viewModel.onBirthDateChange(sdf.format(it))
                    }
                    showDatePicker = false
                }) { Text("Aceptar") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
