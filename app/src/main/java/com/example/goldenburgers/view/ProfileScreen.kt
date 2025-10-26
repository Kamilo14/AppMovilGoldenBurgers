package com.example.goldenburgers.view



import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.ThemeManager
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.CatalogViewModel
import kotlinx.coroutines.launch

/**
 * Esta es la pantalla de Perfil del usuario.
 * Desde aquí, el usuario puede acceder a la edición de sus datos, cambiar
 * las preferencias de la app y cerrar su sesión.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    navController: NavController,
    sessionManager: SessionManager,
    themeManager: ThemeManager,
    catalogViewModel: CatalogViewModel
) {
    // Uso `rememberCoroutineScope` para poder lanzar corutinas desde los callbacks de los botones.
    val scope = rememberCoroutineScope()
    // Observo el estado del modo oscuro desde el ThemeManager. La UI reaccionará a sus cambios.
    val isDarkMode by themeManager.isDarkMode.collectAsStateWithLifecycle(initialValue = false)

    Scaffold(
        topBar = {
            // He decidido no ponerle un icono de navegación a esta TopAppBar para que se sienta
            // como una pantalla principal, consistente con las otras pestañas del BottomNav.
            TopAppBar(
                title = { Text("Mi Perfil") }
            )
        }
    ) { paddingValues ->
        Surface(modifier = Modifier.fillMaxSize().padding(paddingValues), color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Uso una `Card` para agrupar las opciones de forma visualmente atractiva.
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Datos Personales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(16.dp))
                        // Botón para navegar a la pantalla de edición de perfil.
                        OutlinedButton(
                            onClick = { navController.navigate(AppScreens.EditProfileScreen.route) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Editar mis datos")
                        }

                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp))

                        Text("Preferencias", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Modo Oscuro", style = MaterialTheme.typography.bodyLarge)
                            // El Switch controla la preferencia del tema. Su estado `checked` está
                            // conectado al `isDarkMode` del ThemeManager.
                            Switch(
                                checked = isDarkMode,
                                onCheckedChange = { newSetting ->
                                    // Cuando el usuario pulsa el switch, lanzo una corutina para
                                    // llamar a la función que guarda la nueva preferencia en DataStore.
                                    scope.launch {
                                        themeManager.setDarkMode(newSetting)
                                    }
                                }
                            )
                        }
                    }
                }

                Spacer(Modifier.weight(1f)) // Este Spacer empuja el botón de Cerrar Sesión hacia abajo.

                // Botón de Cerrar Sesión. Lo he hecho de color rojo para darle mas importancia.
                Button(
                    onClick = {
                        // Al cerrar sesión, realizo dos acciones importantes:
                        // 1. Limpio el carrito de compras para que esté vacío la próxima vez que inicie sesión.
                        catalogViewModel.clearCart()
                        // 2. Lanzo una corutina para limpiar los datos de la sesión del SessionManager.
                        scope.launch {
                            sessionManager.clearUserSession()
                            // Finalmente, navego a la pantalla de bienvenida y limpio todo el historial
                            // de navegación anterior (`popUpTo`) para que el usuario no pueda volver atrás.
                            navController.navigate(AppScreens.WelcomeScreen.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                launchSingleTop = true
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ExitToApp, null, modifier = Modifier.padding(end = 8.dp))
                    Text("Cerrar Sesión", style = MaterialTheme.typography.labelLarge)
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
