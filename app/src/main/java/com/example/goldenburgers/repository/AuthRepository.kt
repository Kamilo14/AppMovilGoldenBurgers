package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.dto.RegistrarClienteRequest
import com.example.goldenburgers.model.mapper.toDomain
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

/**
 * Repository para manejo de autenticación con Firebase y registro de usuarios
 */
class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    
    // Usaremos el cliente autenticado dinámicamente, no una instancia estática
    // private val gestionUsuarioApi = RetrofitClient.gestionUsuarioService 

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

            // 2. Obtener el token JWT del usuario recién creado
            val token = firebaseUser.getIdToken(true).await().token
                ?: return Result.failure(Exception("No se pudo obtener el token de autenticación"))

            // 3. Crear un servicio autenticado temporal para esta petición
            val authenticatedApi = AuthenticatedRetrofitClient(token).gestionUsuarioService

            // 4. Registrar cliente en el backend usando el servicio autenticado
            val registrarClienteRequest = RegistrarClienteRequest(
                idUsuario = firebaseUid,
                email = email,
                nombreCliente = nombreCliente,
                telefonoCliente = telefonoCliente
            )

            val response = authenticatedApi.registrarCliente(registrarClienteRequest)

            if (response.isSuccessful && response.body() != null) {
                val clienteDTO = response.body()!!
                Result.success(clienteDTO.toDomain())
            } else {
                // Si falla el registro en backend, eliminar usuario de Firebase para mantener consistencia
                try {
                    firebaseUser.delete().await()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Result.failure(Exception("Error al registrar cliente en backend: ${response.code()} ${response.message()}"))
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
     * Obtener el token JWT de Firebase para autenticarse en el backend
     */
    suspend fun getAuthToken(): String? {
        return try {
            val user = firebaseAuth.currentUser ?: return null
            val tokenResult = user.getIdToken(true).await()
            tokenResult.token
        } catch (e: Exception) {
            e.printStackTrace()
            null
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
