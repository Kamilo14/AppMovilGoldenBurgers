package com.example.goldenburgers.viewmodel

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.goldenburgers.model.SessionManager
import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.repository.AuthRepository
import com.example.goldenburgers.repository.ClienteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

data class EditProfileUiState(
    val nombreCliente: String = "",
    val telefonoCliente: String = "",
    val email: String = "",
    val profileImageUri: String? = null,
    val isLoading: Boolean = true,
    val cliente: Cliente? = null
)

class EditProfileViewModel(
    private val authRepository: AuthRepository,
    private val clienteRepository: ClienteRepository,
    private val sessionManager: SessionManager,
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    init {
        sessionManager.profileImageUriFlow
            .onEach { uri -> _uiState.update { it.copy(profileImageUri = uri) } }
            .launchIn(viewModelScope)
    }

    fun loadCurrentUser() {
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            val firebaseUser = authRepository.getCurrentUser()
            if (firebaseUser != null) {
                clienteRepository.obtenerClientePorFirebaseUid(firebaseUser.uid).fold(
                    onSuccess = { cliente ->
                        _uiState.update {
                            it.copy(
                                cliente = cliente,
                                nombreCliente = cliente.nombreCliente,
                                telefonoCliente = cliente.telefonoCliente ?: "",
                                email = cliente.usuario.email,
                                isLoading = false
                            )
                        }
                    },
                    onFailure = { _uiState.update { it.copy(isLoading = false) } }
                )
            } else {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
    
    private fun copyUriToInternalStorage(uriString: String): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(Uri.parse(uriString))
            // [CORRECCIÓN DEFINITIVA] Se usa filesDir (almacenamiento permanente) en lugar de cacheDir (temporal)
            val file = File(context.filesDir, "profile_pic_${System.currentTimeMillis()}.jpg")
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Uri.fromFile(file).toString()
        } catch (e: Exception) {
            null
        }
    }

    fun onNombreClienteChange(nombre: String) = _uiState.update { it.copy(nombreCliente = nombre) }
    fun onTelefonoClienteChange(telefono: String) = _uiState.update { it.copy(telefonoCliente = telefono) }
    fun onProfileImageChange(uri: String?) = _uiState.update { it.copy(profileImageUri = uri) }

    fun saveChanges(onSuccess: () -> Unit, onError: (String) -> Unit) {
        val currentState = _uiState.value
        if (currentState.cliente == null) {
            onError("No se pudo encontrar la información del cliente.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val currentSavedUri = sessionManager.profileImageUriFlow.first()
            if (currentState.profileImageUri != currentSavedUri) {
                currentState.profileImageUri?.let { tempUri ->
                    if (tempUri.startsWith("content://")) { 
                        copyUriToInternalStorage(tempUri)?.let { permanentUri ->
                            sessionManager.saveProfileImageUri(permanentUri)
                        }
                    } else if (tempUri.isBlank()) { // Si el usuario eliminó la foto
                         sessionManager.saveProfileImageUri("")
                    }
                }
            }

            clienteRepository.actualizarPerfil(
                nombreCliente = currentState.nombreCliente,
                email = currentState.email,
                telefonoCliente = currentState.telefonoCliente.ifBlank { null }
            ).fold(
                onSuccess = {
                    _uiState.update { it.copy(isLoading = false) }
                    onSuccess()
                },
                onFailure = { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    onError(exception.message ?: "Error al guardar los cambios")
                }
            )
        }
    }
}