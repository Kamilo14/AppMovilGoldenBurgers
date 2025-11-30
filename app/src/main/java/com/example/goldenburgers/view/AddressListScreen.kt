package com.example.goldenburgers.view

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.AddressViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressListScreen(
    navController: NavController,
    viewModel: AddressViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.loadAddresses()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mis Direcciones") },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { navController.navigate("${AppScreens.EditAddressScreen.route}/-1") }) {
                Icon(Icons.Default.Add, contentDescription = "Añadir nueva dirección")
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (uiState.error != null) {
                Text(uiState.error!!, color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
            } else if (uiState.addresses.isEmpty()) {
                Text("No tienes direcciones guardadas.", modifier = Modifier.align(Alignment.Center))
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    items(uiState.addresses) { address ->
                        val isBeingDeleted = uiState.deletingAddressId == address.idDireccion
                        Card(shape = RoundedCornerShape(12.dp)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = address.alias ?: "Dirección",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))
                                AddressRow(label = "Dirección", value = address.direccion)
                                Spacer(Modifier.height(8.dp))
                                AddressRow(label = "Ciudad", value = address.ciudad.nombreCiudad)
                                Spacer(Modifier.height(16.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                    if (isBeingDeleted) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        IconButton(
                                            onClick = { navController.navigate("${AppScreens.EditAddressScreen.route}/${address.idDireccion}") },
                                            enabled = uiState.deletingAddressId == null // Deshabilitar si otra está en proceso
                                        ) {
                                            Icon(Icons.Default.Edit, "Editar dirección")
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        IconButton(
                                            onClick = { viewModel.deleteAddress(address.idDireccion) },
                                            enabled = uiState.deletingAddressId == null // Deshabilitar si otra está en proceso
                                        ) {
                                            Icon(Icons.Default.Delete, "Eliminar dirección", tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AddressRow(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}