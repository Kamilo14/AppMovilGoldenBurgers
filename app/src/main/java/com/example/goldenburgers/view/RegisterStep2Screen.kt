package com.example.goldenburgers.view

import android.Manifest
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.data.CiudadesDisponibles
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.RegisterViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Segunda pantalla del flujo de registro
 * Recoge datos personales (nombre, teléfono) y dirección del usuario
 * Incluye autocompletado con GPS adaptado al modelo del backend
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep2Screen(navController: NavController, viewModel: RegisterViewModel) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Control del dropdown de ciudades
    var ciudadMenuExpanded by remember { mutableStateOf(false) }

    // Launcher para permisos de ubicación
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@rememberLauncherForActivityResult
                }

                viewModel.onFetchingLocationChange(true)

                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            try {
                                val geocoder = Geocoder(context, Locale.getDefault())
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    geocoder.getFromLocation(location.latitude, location.longitude, 1) { addresses ->
                                        addresses.firstOrNull()?.let { updateAddressFields(viewModel, it, context) }
                                        viewModel.onFetchingLocationChange(false)
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    addresses?.firstOrNull()?.let { updateAddressFields(viewModel, it, context) }
                                    viewModel.onFetchingLocationChange(false)
                                }
                                Toast.makeText(context, "Dirección encontrada", Toast.LENGTH_SHORT).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "No se pudo encontrar una dirección.", Toast.LENGTH_SHORT).show()
                                viewModel.onFetchingLocationChange(false)
                            }
                        } else {
                            Toast.makeText(context, "No se pudo obtener la ubicación.", Toast.LENGTH_SHORT).show()
                            viewModel.onFetchingLocationChange(false)
                        }
                    }
                    .addOnFailureListener {
                        Toast.makeText(context, "Error al obtener la ubicación.", Toast.LENGTH_SHORT).show()
                        viewModel.onFetchingLocationChange(false)
                    }
            } else {
                Toast.makeText(context, "Permiso de ubicación denegado.", Toast.LENGTH_SHORT).show()
            }
        }
    )

    // Validación del paso 2
    val isStep2Valid = uiState.fullName.isNotBlank() &&
            uiState.phoneNumber.isNotBlank() &&
            uiState.direccion.isNotBlank() &&
            uiState.idCiudad != null &&
            uiState.fullNameError == null &&
            uiState.phoneNumberError == null &&
            uiState.direccionError == null &&
            uiState.ciudadError == null

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Datos Personales y Dirección") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                }
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            color = MaterialTheme.colorScheme.background
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 32.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(
                    progress = { 0.4f },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                )

                Text(
                    "Ahora, un poco sobre ti",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Necesitamos tus datos personales y de despacho.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(32.dp))

                // Nombre completo
                OutlinedTextField(
                    value = uiState.fullName,
                    onValueChange = viewModel::onFullNameChange,
                    label = { Text("Nombre Completo") },
                    isError = uiState.fullNameError != null,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = uiState.fullNameError != null) {
                    Text(
                        uiState.fullNameError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Teléfono
                OutlinedTextField(
                    value = uiState.phoneNumber,
                    onValueChange = viewModel::onPhoneNumberChange,
                    label = { Text("Teléfono (9 dígitos)") },
                    isError = uiState.phoneNumberError != null,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                AnimatedVisibility(visible = uiState.phoneNumberError != null) {
                    Text(
                        uiState.phoneNumberError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // Botón GPS
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "Autocompletar con GPS",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    ) {
                        if (uiState.isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = "Autocompletar Dirección")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Dropdown de Ciudades
                ExposedDropdownMenuBox(
                    expanded = ciudadMenuExpanded,
                    onExpandedChange = { ciudadMenuExpanded = !ciudadMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = uiState.idCiudad?.let { CiudadesDisponibles.obtenerNombrePorId(it) } ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ciudad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ciudadMenuExpanded) },
                        isError = uiState.ciudadError != null,
                        modifier = Modifier.fillMaxWidth().menuAnchor(),
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                    )
                    ExposedDropdownMenu(
                        expanded = ciudadMenuExpanded,
                        onDismissRequest = { ciudadMenuExpanded = false }
                    ) {
                        CiudadesDisponibles.ciudades.forEach { ciudad ->
                            DropdownMenuItem(
                                text = { Text(ciudad.nombre) },
                                onClick = {
                                    viewModel.onCiudadChange(ciudad.id)
                                    ciudadMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                AnimatedVisibility(visible = uiState.ciudadError != null) {
                    Text(
                        uiState.ciudadError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Dirección completa
                OutlinedTextField(
                    value = uiState.direccion,
                    onValueChange = viewModel::onDireccionChange,
                    label = { Text("Dirección completa") },
                    placeholder = { Text("Ej: Avenida siempre viva 445") },
                    isError = uiState.direccionError != null,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2
                )
                AnimatedVisibility(visible = uiState.direccionError != null) {
                    Text(
                        uiState.direccionError ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp)
                    )
                }

                Spacer(Modifier.height(16.dp))

                // Alias (opcional)
                OutlinedTextField(
                    value = uiState.alias,
                    onValueChange = viewModel::onAliasChange,
                    label = { Text("Alias (opcional)") },
                    placeholder = { Text("Ej: Casa, Trabajo, Depto") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = {
                        if (isStep2Valid) {
                            navController.navigate(AppScreens.RegisterStep3Screen.route)
                        }
                    },
                    enabled = isStep2Valid,
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Siguiente")
                }
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/**
 * Función auxiliar para autocompletar campos desde GPS
 * Adaptada al modelo del backend con ciudades y dirección completa
 */
private fun updateAddressFields(
    viewModel: RegisterViewModel,
    address: Address,
    context: android.content.Context
) {
    // Construir dirección completa
    val direccionCompleta = buildString {
        address.thoroughfare?.let { append(it) }
        address.subThoroughfare?.let {
            if (isNotEmpty()) append(" ")
            append(it)
        }
        address.premises?.let {
            if (isNotEmpty()) append(", ")
            append(it)
        }
    }

    viewModel.onDireccionChange(direccionCompleta)

    // Intentar encontrar la ciudad en el catálogo
    val ciudadDetectada = address.locality ?: address.subAdminArea ?: ""
    val idCiudad = CiudadesDisponibles.encontrarCiudad(ciudadDetectada)

    if (idCiudad != null) {
        viewModel.onCiudadChange(idCiudad)
    } else {
        Toast.makeText(
            context,
            "Ciudad '$ciudadDetectada' no encontrada. Por favor selecciona manualmente.",
            Toast.LENGTH_LONG
        ).show()
    }

    // Sugerir alias basado en el tipo de lugar
    val tipoLugar = address.featureName ?: ""
    val aliasSugerido = when {
        tipoLugar.contains("casa", ignoreCase = true) -> "Casa"
        tipoLugar.contains("work", ignoreCase = true) ||
        tipoLugar.contains("office", ignoreCase = true) -> "Trabajo"
        else -> ""
    }

    if (aliasSugerido.isNotEmpty()) {
        viewModel.onAliasChange(aliasSugerido)
    }
}
