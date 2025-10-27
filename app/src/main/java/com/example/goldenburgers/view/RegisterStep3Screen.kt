package com.example.goldenburgers.view
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.goldenburgers.navigation.AppScreens
import com.example.goldenburgers.viewmodel.RegisterViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Esta es la tercera pantalla del flujo de registro, dedicada a la foto de perfil.
 * Es una pantalla opcional que le permite al usuario subir una foto desde la galería o la cámara.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterStep3Screen(navController: NavController, viewModel: RegisterViewModel) {

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Guardo aquí la URI de la imagen temporal que se crea para la cámara.
    // Es necesario para poder acceder a ella en el callback del `cameraLauncher`.
    var tempImageUri by remember { mutableStateOf<Uri?>(null) }

    // --- LANZADORES DE ACTIVIDADES ---
    // La forma moderna y segura de manejar permisos y resultados de otras apps en Compose.

    // Launcher para la galería.
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let { viewModel.onProfileImageChange(it.toString()) }
    }

    // Launcher para la cámara.
    val cameraLauncher = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempImageUri?.let { viewModel.onProfileImageChange(it.toString()) }
        }
    }

    // Launcher para el permiso de la cámara.
    val cameraPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            isGranted ->
        if (isGranted) {
            // Si el usuario concede el permiso, creo una URI y lanzo la cámara.
            val uri = createImageUri(context)
            tempImageUri = uri
            cameraLauncher.launch(uri)
        } else {
            Toast.makeText(context, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Foto de Perfil (Opcional)") }, navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver") } }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LinearProgressIndicator(progress = { 0.6f }, modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))

            Text("Sube una foto de perfil", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text("Esto ayudará a tus amigos a reconocerte. ¡Sonríe!", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(32.dp))

            // Vista previa de la imagen de perfil.
            Box(
                modifier = Modifier.size(150.dp).clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant).border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.profileImageUri != null) {
                    // Uso la librería Coil para cargar la imagen de forma asíncrona desde su URI.
                    // Coil es muy potente y maneja la carga, el cacheo y la decodificación por mí.
                    Image(painter = rememberAsyncImagePainter(uiState.profileImageUri), "Foto de perfil", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                } else {
                    // Si no hay imagen, muestro un icono por defecto.
                    Icon(Icons.Default.AddAPhoto, "Añadir foto", modifier = Modifier.size(50.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Spacer(Modifier.height(32.dp))

            // Botones para que el usuario elija de dónde sacar la foto.
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedButton(onClick = { galleryLauncher.launch("image/*") }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.AddAPhoto, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Galería")
                }
                Button(onClick = {
                    when (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)) {
                        PackageManager.PERMISSION_GRANTED -> {
                            val uri = createImageUri(context)
                            tempImageUri = uri
                            cameraLauncher.launch(uri)
                        }
                        else -> cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.PhotoCamera, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Cámara")
                }
            }

            Spacer(Modifier.weight(1f)) // Empujo los botones de navegación hacia abajo.

            // --- Botones de Navegación ---
            // El usuario puede continuar al siguiente paso con o sin foto.
            Button(onClick = { navController.navigate(AppScreens.RegisterStep4Screen.route) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                Text("Siguiente")
            }
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = { navController.navigate(AppScreens.RegisterStep4Screen.route) }) {
                Text("Omitir por ahora")
            }

            Spacer(Modifier.height(24.dp))
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
    return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", imageFile)
}
