package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.dto.RegistrarClienteRequest
import com.example.goldenburgers.model.mapper.toDomain
import com.example.goldenburgers.service.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Repository para manejo de autenticación con Firebase y registro de usuarios
 */
class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val gestionUsuarioApi = RetrofitClient.gestionUsuarioService

    /**
     * Registrar un nuevo usuario con Firebase y en el backend
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @param nombreCliente Nombre completo del cliente
     * @param telefonoCliente Teléfono del cliente (opcional)
     * @return Cliente registrado con toda su información
     */
    suspend fun registerUser(
        email: String,
        password: String,
        nombreCliente: String,
        telefonoCliente: String?
    ): Result<Cliente> {
        return try {
            // 1. Registrar usuario en Firebase Authentication
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: return Result.failure(Exception("Error al crear usuario en Firebase"))

            val firebaseUid = firebaseUser.uid

            // 2. Registrar cliente en el backend
            val registrarClienteRequest = RegistrarClienteRequest(
                idUsuario = firebaseUid,
                email = email,
                nombreCliente = nombreCliente,
                telefonoCliente = telefonoCliente
            )

            val response = gestionUsuarioApi.registrarCliente(registrarClienteRequest)

            if (response.isSuccessful && response.body() != null) {
                val clienteDTO = response.body()!!
                Result.success(clienteDTO.toDomain())
            } else {
                // Si falla el registro en backend, eliminar usuario de Firebase
                firebaseUser.delete().await()
                Result.failure(Exception("Error al registrar cliente: ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Login de usuario con Firebase
     *
     * @param email Email del usuario
     * @param password Contraseña del usuario
     * @return FirebaseUser si el login es exitoso
     */
    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
                ?: return Result.failure(Exception("Usuario no encontrado"))

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Cerrar sesión
     */
    fun logout() {
        firebaseAuth.signOut()
    }

    /**
     * Obtener usuario actual de Firebase
     */
    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }

    /**
     * Obtener UID del usuario actual
     */
    fun getCurrentUserUid(): String? {
        return firebaseAuth.currentUser?.uid
    }
}
