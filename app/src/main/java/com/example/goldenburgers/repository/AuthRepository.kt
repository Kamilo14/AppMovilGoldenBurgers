package com.example.goldenburgers.repository

import com.example.goldenburgers.model.data.Cliente
import com.example.goldenburgers.model.dto.LoginRequest
import com.example.goldenburgers.model.dto.RegistrarClienteRequest
import com.example.goldenburgers.model.mapper.toDomain
import com.example.goldenburgers.service.AuthenticatedRetrofitClient
import com.example.goldenburgers.service.RetrofitClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

// Data class para devolver múltiples valores desde registerUser
data class RegistrationResult(val cliente: Cliente, val internalToken: String)

class AuthRepository {

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val apiGatewayService = RetrofitClient.apiGatewayService

    /**
     * [CORREGIDO] Registra un usuario y devuelve tanto el cliente como el token interno.
     */
    suspend fun registerUser(
        email: String,
        password: String,
        nombreCliente: String,
        telefonoCliente: String?
    ): Result<RegistrationResult> {
        return try {
            // 1. Registrar en Firebase
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: return Result.failure(Exception("Error al crear usuario en Firebase"))

            // 2. Registrar en el backend
            val authenticatedApi = AuthenticatedRetrofitClient(firebaseUser.getIdToken(true).await().token!!).gestionUsuarioService
            val registrarClienteRequest = RegistrarClienteRequest(idUsuario = firebaseUser.uid, email = email, nombreCliente = nombreCliente, telefonoCliente = telefonoCliente)
            val response = authenticatedApi.registrarCliente(registrarClienteRequest)

            if (!response.isSuccessful || response.body() == null) {
                firebaseUser.delete().await() // Rollback
                return Result.failure(Exception("Error al registrar cliente en backend: ${response.code()}"))
            }
            val cliente = response.body()!!.toDomain()

            // 3. Intercambiar token de Firebase por token interno
            val firebaseToken = firebaseUser.getIdToken(false).await().token!!
            val exchangeResult = exchangeToken(firebaseToken)

            exchangeResult.fold(
                onSuccess = { internalToken ->
                    Result.success(RegistrationResult(cliente, internalToken))
                },
                onFailure = {
                    firebaseUser.delete().await() // Rollback
                    Result.failure(Exception("Usuario registrado, pero falló el intercambio de token."))
                }
            )
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("Usuario no encontrado en Firebase"))
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun exchangeToken(firebaseToken: String): Result<String> {
        return try {
            val request = LoginRequest(firebaseToken = firebaseToken)
            val response = apiGatewayService.login(request)

            if (response.isSuccessful && response.body() != null) {
                val internalToken = response.body()!!.internalToken
                if (internalToken.isNotBlank()) {
                    Result.success(internalToken)
                } else {
                    Result.failure(Exception("El backend devolvió un token interno vacío."))
                }
            } else {
                Result.failure(Exception("Falló el intercambio de token: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getFirebaseToken(): String? {
        return try {
            firebaseAuth.currentUser?.getIdToken(true)?.await()?.token
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        firebaseAuth.signOut()
    }

    fun getCurrentUser(): FirebaseUser? {
        return firebaseAuth.currentUser
    }
}
