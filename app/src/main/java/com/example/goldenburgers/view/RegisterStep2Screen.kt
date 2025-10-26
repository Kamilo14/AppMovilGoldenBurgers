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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.RegisterViewModel
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import java.util.Locale

/**
 * Esta es la segunda pantalla del flujo de registro.
 * Aquí recojo tanto los datos personales (nombre, teléfono) como la dirección del usuario.
 * También he implementado una funcionalidad de autocompletado con GPS para facilitar el proceso.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep2Screen(navController: NavController, viewModel: RegisterViewModel) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    // `fusedLocationClient` es el cliente principal de los servicios de ubicación de Google.
    // Lo usaré para obtener la ubicación actual del dispositivo.
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)

    // Este es el launcher que maneja la petición de permisos de ubicación.
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
        onResult = { permissions ->
            // El resultado me llega como un mapa de permisos y si fueron concedidos (true/false).
            if (permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)) {

                // Doble chequeo de seguridad para asegurarme de que tengo el permiso antes de proceder.
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return@rememberLauncherForActivityResult
                }

                // Le digo al ViewModel que empiece a mostrar la señal de carga.
                viewModel.onFetchingLocationChange(true)

                // Pido la ubicación actual con alta precisión. Esta es una tarea asíncrona.
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener { location ->
                        // Si la ubicación se obtiene con éxito, este listener se ejecuta.
                        if (location != null) {
                            try {
                                // El Geocoder es el servicio que traduce coordenadas (lat, lon) a una dirección física.
                                val geocoder = Geocoder(context, Locale.getDefault())
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    // A partir de Android 13, se debe usar la versión asíncrona de getFromLocation.
                                    geocoder.getFromLocation(location.latitude, location.longitude, 1) {
                                            addresses ->
                                        // Una vez tengo la dirección, llamo a mi función auxiliar para rellenar los campos.
                                        addresses.firstOrNull()?.let { updateAddressFields(viewModel, it) }
                                        viewModel.onFetchingLocationChange(false) // Termino la carga.
                                    }
                                } else {
                                    // Para versiones antiguas de Android, uso la versión síncrona (deprecated).
                                    @Suppress("DEPRECATION")
                                    val addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                                    addresses?.firstOrNull()?.let { updateAddressFields(viewModel, it) }
                                    viewModel.onFetchingLocationChange(false) // Termino la carga.
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

    val isStep2Valid = uiState.fullName.isNotBlank() && uiState.phoneNumber.isNotBlank() &&
            uiState.street.isNotBlank() && uiState.number.isNotBlank() &&
            uiState.city.isNotBlank() && uiState.commune.isNotBlank() && uiState.region.isNotBlank() &&
            uiState.fullNameError == null && uiState.phoneNumberError == null &&
            uiState.streetError == null && uiState.numberError == null &&
            uiState.cityError == null && uiState.communeError == null && uiState.regionError == null

    Scaffold(
        topBar = { TopAppBar(title = { Text("Datos Personales y Dirección") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                LinearProgressIndicator(progress = { 0.4f }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

                Text("Ahora, un poco sobre ti", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
                Spacer(Modifier.height(8.dp))
                Text("Necesitamos tus datos personales y de despacho.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(32.dp))

                OutlinedTextField(value = uiState.fullName, onValueChange = viewModel::onFullNameChange, label = { Text("Nombre Completo") }, isError = uiState.fullNameError != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                AnimatedVisibility(visible = uiState.fullNameError != null) {
                    Text(uiState.fullNameError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                }
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.phoneNumber, onValueChange = viewModel::onPhoneNumberChange, label = { Text("Teléfono (9 dígitos)") }, isError = uiState.phoneNumberError != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                AnimatedVisibility(visible = uiState.phoneNumberError != null) {
                    Text(uiState.phoneNumberError ?: "", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 4.dp))
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Autocompletar con GPS", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        // Cuando se pulsa el botón, lanzo la petición de permisos.
                        locationPermissionLauncher.launch(arrayOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                        ))
                    }) {
                        // Aquí muestro el indicador de carga si la app está buscando la ubicación.
                        if (uiState.isFetchingLocation) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Icon(Icons.Default.LocationOn, contentDescription = "Autocompletar Dirección")
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(value = uiState.street, onValueChange = viewModel::onStreetChange, label = { Text("Calle") }, isError = uiState.streetError != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.number, onValueChange = viewModel::onNumberChange, label = { Text("Número") }, isError = uiState.numberError != null, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.commune, onValueChange = viewModel::onCommuneChange, label = { Text("Comuna") }, isError = uiState.communeError != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.city, onValueChange = viewModel::onCityChange, label = { Text("Ciudad") }, isError = uiState.cityError != null, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.region, onValueChange = viewModel::onRegionChange, label = { Text("Región") }, isError = uiState.regionError != null, singleLine = true, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(32.dp))

                Button(
                    onClick = { if (isStep2Valid) { navController.navigate(AppScreens.RegisterStep3Screen.route) } },
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
 * He creado esta función auxiliar para mantener el código más limpio. Su única
 * responsabilidad es tomar un objeto `Address` y llamar a las funciones del ViewModel
 * para actualizar los campos de texto correspondientes.
 */
private fun updateAddressFields(viewModel: RegisterViewModel, address: Address) {
    viewModel.onStreetChange(address.thoroughfare ?: "")
    viewModel.onNumberChange(address.subThoroughfare ?: "")
    viewModel.onCommuneChange(address.locality ?: "")
    viewModel.onCityChange(address.subAdminArea ?: "")
    viewModel.onRegionChange(address.adminArea ?: "")
}
