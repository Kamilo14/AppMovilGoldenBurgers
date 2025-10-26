package com.example.goldenburgers.view


import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddAPhoto
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.goldenburgers.viewmodel.EditProfileViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Esta es la pantalla donde el usuario puede editar su información personal.
 * Sigue el patrón MVVM, recibiendo un `viewModel` que se encarga de toda la lógica.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProfileScreen(
    navController: NavController,
    viewModel: EditProfileViewModel
) {
    // Observo el estado (uiState) del ViewModel. Cada vez que el estado cambie,
    // esta pantalla se recompondrá automáticamente para reflejar los nuevos datos.
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Esta variable temporal es necesaria para la cámara. Guarda la URI del archivo donde
    // la cámara debe guardar la foto, para poder recuperarla después en el callback.
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // He usado un LaunchedEffect con `Unit` como clave. Esto hace que el bloque de código
    // se ejecute una sola vez, justo cuando la pantalla se muestra por primera vez.
    // Es el lugar perfecto para decirle al ViewModel que empiece a cargar los datos del usuario.
    LaunchedEffect(Unit) {
        viewModel.loadCurrentUser()
    }

    // --- LANZADORES DE ACTIVIDADES ---
    // Aquí defino los "launchers" que me permiten interactuar con otras apps (Galería, Cámara)
    // y recibir un resultado de vuelta. Es la forma moderna y segura de hacerlo en Compose.

    // Launcher para seleccionar una imagen de la galería.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        // Cuando el usuario selecciona una imagen, este callback se ejecuta. Le paso la URI
        // al ViewModel para que actualice la vista previa.
        uri?.let { viewModel.onProfileImageChange(it.toString()) }
    }

    // Launcher para tomar una foto con la cámara.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        // Este callback se ejecuta después de que el usuario toma la foto. Si `success` es true,
        // significa que la foto se guardó correctamente en la `tempImageUri` que le pasamos.
        if (success) { tempImageUri?.let { viewModel.onProfileImageChange(it.toString()) } }
    }

    // Launcher para pedir el permiso de la cámara.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
        if (isGranted) {
            // Si el usuario concede el permiso, creo una nueva URI y lanzo la cámara inmediatamente.
            val uri = createImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            // Si lo deniega, le muestro un mensaje para que sepa por qué no funciona.
            Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    // --- INTERFAZ DE USUARIO ---
    Scaffold(
        topBar = { TopAppBar(title = { Text("Editar Perfil") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        // Muestro un indicador de carga mientras el ViewModel está buscando los datos del usuario.
        // Esto mejora mucho la experiencia de usuario (UX).
        if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        } else {
            // Cuando la carga termina, muestro el formulario completo.
            Column(
                modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp).verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // --- Vista previa de la imagen y botones ---
                Box(modifier = Modifier.size(150.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape)) {
                    if (uiState.profileImageUri != null) {
                        // Uso la librería Coil para cargar la imagen de forma asíncrona a partir de su URI.
                        Image(painter = rememberAsyncImagePainter(uiState.profileImageUri), "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    } else {
                        Icon(Icons.Default.AddAPhoto, "Añadir foto", modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.AddAPhoto, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Galería")
                    }
                    Button(onClick = {
                        // Lógica para el botón de la cámara: primero compruebo si ya tengo el permiso.
                        when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                            PackageManager.PERMISSION_GRANTED -> {
                                // Si ya lo tengo, creo una URI y lanzo la cámara.
                                val uri = createImageUri(context)
                                tempImageUri = uri
                                cameraLauncher.launch(uri)
                            }
                            else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA) // Si no, lanzo el diálogo para pedirlo.
                        }
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.PhotoCamera, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Cámara")
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 24.dp))

                // --- Campos de texto para los datos del usuario ---
                // Cada OutlinedTextField está conectado al estado del ViewModel.
                // `value` lee el dato del estado, y `onValueChange` llama a la función correspondiente
                // en el ViewModel para actualizarlo. Esto es la base de un flujo de datos unidireccional.
                OutlinedTextField(value = uiState.fullName, onValueChange = viewModel::onFullNameChange, label = { Text("Nombre Completo") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.phoneNumber, onValueChange = viewModel::onPhoneNumberChange, label = { Text("Teléfono") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.street, onValueChange = viewModel::onStreetChange, label = { Text("Calle") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.number, onValueChange = viewModel::onNumberChange, label = { Text("Número") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.city, onValueChange = viewModel::onCityChange, label = { Text("Ciudad") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.commune, onValueChange = viewModel::onCommuneChange, label = { Text("Comuna") }, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(value = uiState.region, onValueChange = viewModel::onRegionChange, label = { Text("Región") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(32.dp))

                // Botón para guardar los cambios. Llama a la función del ViewModel y maneja
                // los callbacks de éxito o error para dar feedback al usuario.
                Button(
                    onClick = {
                        viewModel.saveChanges(
                            onSuccess = {
                                Toast.makeText(context, "Datos guardados con éxito", Toast.LENGTH_SHORT).show()
                                navController.popBackStack() // Vuelvo a la pantalla de perfil.
                            },
                            onError = { error -> Toast.makeText(context, "Error: $error", Toast.LENGTH_LONG).show() }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    Text("Guardar Cambios")
                }
            }
        }
    }
}

/**
 * Esta función auxiliar es crucial para la cámara. Crea un archivo temporal y seguro
 * en la caché de la aplicación y devuelve una URI para ese archivo.
 * El `FileProvider` es la pieza clave que permite compartir esta URI con la app de la cámara
 * de forma segura, evitando el error `FileUriExposedException` en versiones modernas de Android.
 */
private fun createImageUri(context: Context): Uri {
    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    val imageFile = File.createTempFile("JPEG_${timeStamp}_", ".jpg", context.cacheDir)
    return FileProvider.getUriForFile(context, "${context.packageName}.provider", imageFile)
}

