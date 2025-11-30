package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.viewmodel.EditAddressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAddressScreen(
    navController: NavController,
    viewModel: EditAddressViewModel,
    addressId: Long
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var ciudadMenuExpanded by remember { mutableStateOf(false) }

    // [CORREGIDO] Carga los datos iniciales (ciudades) y la dirección a editar
    LaunchedEffect(Unit) {
        viewModel.loadInitialData()
        viewModel.loadAddress(addressId)
    }

    // Observa el estado `finishScreen` para navegar hacia atrás automáticamente
    LaunchedEffect(uiState.finishScreen) {
        if (uiState.finishScreen) {
            navController.popBackStack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isNewAddress) "Nueva Dirección" else "Editar Dirección") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        }
    ) { paddingValues ->
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Campo para Alias
                OutlinedTextField(
                    value = uiState.alias,
                    onValueChange = viewModel::onAliasChange,
                    label = { Text("Alias (Ej: Casa, Trabajo)") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))

                // Menú desplegable para Ciudades
                ExposedDropdownMenuBox(
                    expanded = ciudadMenuExpanded,
                    onExpandedChange = { ciudadMenuExpanded = !ciudadMenuExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val selectedCiudad = uiState.availableCiudades.find { it.idCiudad == uiState.ciudadId }
                    OutlinedTextField(
                        value = selectedCiudad?.nombreCiudad ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ciudad") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = ciudadMenuExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = ciudadMenuExpanded,
                        onDismissRequest = { ciudadMenuExpanded = false }
                    ) {
                        uiState.availableCiudades.forEach { ciudad ->
                            DropdownMenuItem(
                                text = { Text(ciudad.nombreCiudad) },
                                onClick = {
                                    viewModel.onCiudadSelected(ciudad.idCiudad)
                                    ciudadMenuExpanded = false
                                }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Campo para Dirección
                OutlinedTextField(
                    value = uiState.direccion,
                    onValueChange = viewModel::onDireccionChange,
                    label = { Text("Dirección Completa") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
                Spacer(Modifier.height(24.dp))

                // Botón para Guardar
                Button(
                    onClick = { viewModel.saveAddress(addressId) },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Guardar Dirección")
                }

                // Mostrar errores
                uiState.error?.let {
                    Spacer(Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                    LaunchedEffect(it) {
                        Toast.makeText(context, it, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }
}